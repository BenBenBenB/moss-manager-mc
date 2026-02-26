# GUI Development Guide

Minecraft 1.21.11 · Fabric Loader 0.18.3 · Fabric API 0.140.2 · Yarn mappings 1.21.11+build.3

All signatures in this document are verified against the actual compiled JARs for this version. When in doubt, inspect the cached Loom JARs directly:

```bash
# Find the intermediary class name in the yarn mappings:
grep "ClassName" ~/.gradle/caches/fabric-loom/1.21.11/net.fabricmc.yarn.1_21_11.1.21.11+build.3-v2/mappings.tiny

# Inspect a class using its intermediary name (e.g. giy = EntryListWidget):
javap -cp ~/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar <intermediary-name>
```

---

## Project structure for GUI code

GUI code belongs in `mossman-gui`. It is client-only (`environment: "client"` in `fabric.mod.json`) and must never be loaded on a dedicated server. Access core data through `MossManApi`.

```
mossman-gui/src/main/java/com/mossman/
├── MossManGuiMod.java          # ClientModInitializer — register client commands here
└── gui/
    └── screens/                # One file per Screen subclass
```

---

## Screens

Extend `net.minecraft.client.gui.screen.Screen`.

```java
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class MyScreen extends Screen {

    private final Screen parent;

    public MyScreen(Screen parent) {
        super(Text.literal("My Screen Title"));
        this.parent = parent;
    }

    // Called whenever the screen is (re)initialized — when first opened and on window resize.
    // Add all widgets here. Do not cache widgets as fields before init() is called.
    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20)
                .build());
    }

    // Render order: super.render() draws background + all child widgets, then draw custom text on top.
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
    }

    // Called on ESC. Default calls client.setScreen(null) which closes the screen.
    @Override
    public void close() {
        client.setScreen(parent);   // pass null if there is no parent
    }

    // Return false to keep the game ticking (music, animations) while the screen is open.
    // Default returns true (pauses single-player).
    @Override
    public boolean shouldPause() {
        return false;
    }
}
```

### Fields available inside a Screen

| Field | Type | Description |
|---|---|---|
| `width` | `int` | Current screen width in pixels |
| `height` | `int` | Current screen height in pixels |
| `client` | `MinecraftClient` | The Minecraft client instance |
| `textRenderer` | `TextRenderer` | Font renderer — pass to DrawContext text methods |
| `title` | `Text` | The text passed to `super(Text)` |

### Opening a screen

```java
MinecraftClient.getInstance().setScreen(new MyScreen(null));
```

Call this from the main/render thread, or schedule it with `client.execute(() -> ...)` if calling from a command thread.

---

## Client-side commands

Use `ClientCommandRegistrationCallback` to register commands that run on the client only (no server round-trip). Good for opening screens.

```java
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
        dispatcher.register(
                ClientCommandManager.literal("mymod")
                        .then(ClientCommandManager.literal("gui")
                                .executes(ctx -> {
                                    MinecraftClient client = MinecraftClient.getInstance();
                                    client.execute(() -> client.setScreen(new MyScreen(null)));
                                    return 1;
                                }))
        )
);
```

Register this in `ClientModInitializer.onInitializeClient()`.

Client commands take priority over same-name server commands. They only work when a client-side mod is installed; they are invisible to the server.

---

## Widgets

All widgets are added to a screen via `addDrawableChild(widget)` inside `init()`. The screen owns their lifecycle — you do not need to call render on them manually.

### ButtonWidget

```java
import net.minecraft.client.gui.widget.ButtonWidget;

ButtonWidget button = ButtonWidget.builder(Text.literal("Click me"), btn -> {
            // action on press
        })
        .dimensions(x, y, width, height)   // required
        .tooltip(Tooltip.of(Text.literal("Tooltip text")))  // optional
        .build();

addDrawableChild(button);
```

Default button size: width=200, height=20. Minimum practical width: ~60.

### TextFieldWidget

```java
import net.minecraft.client.gui.widget.TextFieldWidget;

TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal("hint"));
field.setMaxLength(64);
field.setChangedListener(text -> { /* called on every keystroke */ });
field.setText("initial value");

addDrawableChild(field);

// Read current value:
String value = field.getText();
```

TextFieldWidget does not capture keyboard focus automatically. To make it focused when the screen opens, call `setInitialFocus(field)` at the end of `init()`.

### CheckboxWidget

```java
import net.minecraft.client.gui.widget.CheckboxWidget;

CheckboxWidget checkbox = CheckboxWidget.builder(Text.literal("Enable feature"), checked)
        .position(x, y)
        .callback((box, nowChecked) -> { /* called when toggled */ })
        .build();

addDrawableChild(checkbox);

boolean isChecked = checkbox.isChecked();
```

### CyclingButtonWidget

Cycles through a fixed list of values on each click.

```java
import net.minecraft.client.gui.widget.CyclingButtonWidget;

// Second arg to builder() is the initial value (required in 1.21.11 — single-arg overload was removed).
CyclingButtonWidget<String> cycler = CyclingButtonWidget.<String>builder(Text::literal, "Option A")
        .values("Option A", "Option B", "Option C")
        .build(x, y, width, height, Text.literal("Mode"), (btn, value) -> {
            // called when the value changes
        });

addDrawableChild(cycler);
```

---

## Scrollable list (AlwaysSelectedEntryListWidget)

Use this for any scrollable list where exactly one item should always be selected (project list, player list, etc.). For a list where nothing needs to be selected, extend `EntryListWidget` instead.

### Constructor

```java
new AlwaysSelectedEntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight)
```

| Parameter | Meaning |
|---|---|
| `width` | Total pixel width of the widget (usually `Screen.this.width`) |
| `height` | Pixel height of the scrollable area |
| `y` | Top edge Y coordinate |
| `itemHeight` | Height of each row in pixels |

### Full example

```java
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;

private class MyListWidget extends AlwaysSelectedEntryListWidget<MyListWidget.MyEntry> {

    public MyListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, List<MyItem> items) {
        super(client, width, height, y, itemHeight);
        for (MyItem item : items) {
            addEntry(new MyEntry(item));
        }
    }

    // How wide each row appears (controls text wrap and highlight width).
    // Subtract ~20px to leave room for the scrollbar.
    @Override
    public int getRowWidth() {
        return MyScreen.this.width - 20;
    }

    // X position of the scrollbar.
    @Override
    protected int getScrollbarX() {
        return MyScreen.this.width / 2 + getRowWidth() / 2 + 4;
    }

    public class MyEntry extends AlwaysSelectedEntryListWidget.Entry<MyEntry> {

        private final MyItem item;

        public MyEntry(MyItem item) {
            this.item = item;
        }

        // render() receives the mouse position, NOT the entry's own position.
        // Use getContentX() / getContentY() for the entry's top-left drawing origin.
        // itemHeight is set on the widget constructor — size your drawing to fit within it.
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int x = getContentX();
            int y = getContentY();

            // Primary line (white)
            context.drawTextWithShadow(MyScreen.this.textRenderer, Text.literal(item.getName()), x, y + 2, 0xFFFFFF);
            // Secondary line (grey) — only fits if itemHeight >= 22
            context.drawTextWithShadow(MyScreen.this.textRenderer, Text.literal(item.getDescription()), x, y + 13, 0xAAAAAA);
        }

        // Screen reader / narration text for this entry.
        @Override
        public Text getNarration() {
            return Text.literal(item.getName());
        }
    }
}
```

Add the widget to the screen in `init()`:

```java
myList = new MyListWidget(client, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, 24, items);
addDrawableChild(myList);
```

### Item height guidelines

| `itemHeight` | What fits |
|---|---|
| 12 | One line of text |
| 20 | One line + comfortable padding |
| 24 | Two lines of text (the standard) |
| 36 | Two lines + an icon or progress bar |

---

## DrawContext drawing methods

`DrawContext` is passed to every `render` call. Use it for all custom drawing.

### Text

```java
// Left-aligned with shadow
context.drawTextWithShadow(textRenderer, Text.literal("Hello"), x, y, 0xFFFFFF);

// Centered with shadow
context.drawCenteredTextWithShadow(textRenderer, Text.literal("Title"), centerX, y, 0xFFFFFF);

// Left-aligned, no shadow
context.drawText(textRenderer, Text.literal("Dim"), x, y, 0xAAAAAA, false);
```

### Rectangles

```java
// Filled rectangle
context.fill(x1, y1, x2, y2, 0xFF000000);              // opaque black

// Horizontal/vertical gradient
context.fillGradient(x1, y1, x2, y2, topColor, bottomColor);

// Outline only
context.drawBorder(x, y, width, height, 0xFFFFFFFF);
```

### Textures

```java
// Slice from a sprite sheet (u/v are pixel offsets into the texture)
context.drawTexture(identifier, destX, destY, u, v, width, height);

// Nine-slice GUI texture (scales without distorting corners — used for panels)
context.drawGuiTexture(identifier, x, y, width, height);
```

`Identifier` is created with `Identifier.of("mossman-gui", "textures/gui/my_panel.png")`. Texture files live under `src/main/resources/assets/<namespace>/`.

### Items

```java
context.drawItem(itemStack, x, y);              // renders the item sprite
context.drawItemTooltip(textRenderer, itemStack, mouseX, mouseY);  // renders its tooltip
```

### Colors

Colors are packed 32-bit ARGB integers:

| Value | Result |
|---|---|
| `0xFFFFFFFF` | Opaque white |
| `0xFF000000` | Opaque black |
| `0xAAAAAAAAAA` | 67% transparent grey |
| `0xFF_FF4444` | Opaque red |
| `0x00_000000` | Fully transparent (invisible) |

---

## Key tips

- **`init()` is called on every resize.** Do not hold state in widget fields that survives resize — rebuild from data in `init()`.
- **Database calls in `init()` block the render thread.** For a basic start this is fine; for large data sets, load async and re-initialize when done.
- **`textRenderer` is not available until `init()` runs.** Do not use it in the constructor.
- **`client.player` may be null** on the title screen. Always null-check before accessing player-specific data.
- **`GameProfile` is a Java record** — use `getGameProfile().name()` and `getGameProfile().id()`, not `.getName()` / `.getId()`. Authlib 7.x switched to record accessor methods.
- **Client commands vs. server commands**: If the TUI mod is also installed, both register a `/mossman` node. Client commands (GUI) are dispatched first, so `/mossman gui` is handled client-side even if the server also has `/mossman`.
