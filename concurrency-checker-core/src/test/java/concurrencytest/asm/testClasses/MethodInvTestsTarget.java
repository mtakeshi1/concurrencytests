package concurrencytest.asm.testClasses;

import java.util.ArrayList;
import java.util.List;

public class MethodInvTestsTarget implements Example{

    private List<String> list = new ArrayList<>();

    @Override
    public void a() {
        list.add(0, "".trim());
    }

    @Override
    public Object b() {
        return list.size();
    }

    @Override
    public String c(String arg) {
        list.add(arg.trim());
        return "";
    }
}
