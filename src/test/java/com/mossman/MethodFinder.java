import org.junit.jupiter.api.Test;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;

// debug tool to find the classes for ClickEvent and HoverEvent
public class MethodFinder {
    @Test
    public void findMethods() {
        StringBuilder sb = new StringBuilder();
        sb.append("CLICK_CLASSES:");
        for (Class<?> c : ClickEvent.class.getDeclaredClasses()) {
            sb.append(c.getSimpleName()).append(",");
        }
        sb.append("HOVER_CLASSES:");
        for (Class<?> c : HoverEvent.class.getDeclaredClasses()) {
            sb.append(c.getSimpleName()).append(",");
        }
        //throw new RuntimeException("WANTED: " + sb.toString());
        // keeping around for posible future debug
    }
}
