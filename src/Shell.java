public class Shell {
    public static void main(String[] args) {
        switch (args.length) {
            case 0 -> RahanjiCKt.shell();
            case 1 -> {
                if (args[0].endsWith(".rahanji")) RahanjiCKt.run(args[0]);
                else if (args[0].endsWith(".rj")) RahanjiCKt.compile(args[0]);
                else
                    MethodsKt.fail(new IOError("Specified File name does not end with .rj or .rahanji!", Position.Companion.getUnknown()), "Starting");
            }
            case 2 -> {
                if (!args[0].equals("-direct")) {
                    System.err.println("Unknown option:" + args[0]);
                    System.exit(-1);
                } else if (!args[1].endsWith(".rj"))
                    MethodsKt.fail(new IOError("Specified File name does not end with .rj", Position.Companion.getUnknown()), "Starting");
                else RahanjiCKt.noCompile(args[1]);
            }
            default -> System.out.println("usage: rahanjic <-options> file");
        }
    }
}
