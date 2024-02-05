import org.jetbrains.annotations.NotNull;

public class Shell {
    public static void main(@NotNull String[] args) {
        Thread.currentThread().setName("Starting");
        switch (args.length) {
            case 0 -> TenessScriptKt.shell();
            case 1 -> {
                if (args[0].endsWith(".tsc")) TenessScriptKt.run(args[0]);
                else if (args[0].endsWith(".tss")) TenessScriptKt.compile(args[0]);
                else
                    MethodsKt.fail(new IOError("Specified File name does not end with .tss or .tsc!", Position.Companion.getUnknown()), "Starting");
            }
            case 2 -> {
                if (!args[0].equals("-direct")) {
                    System.err.println("Unknown option:" + args[0]);
                    System.exit(-1);
                } else if (!args[1].endsWith(".tss"))
                    MethodsKt.fail(new IOError("Specified File name does not end with .tss", Position.Companion.getUnknown()), "Starting");
                else TenessScriptKt.noCompile(args[1]);
            }
            default -> System.out.println("usage: Tenessc <-options> file");
        }
    }
}
