package Main;

/** Plain main class lets the bundled launcher load JavaFX from its application jars. */
public final class Launcher {
    private Launcher() { }
    public static void main(String[] args) {
        for(int i=0;i<args.length-1;i++) if(args[i].equals("--data-home")) System.setProperty("store.home",args[i+1]);
        if(System.getProperty("store.home")==null)
            System.setProperty("store.home",java.nio.file.Path.of(System.getProperty("user.home"),".storepos").toString());
        javafx.application.Application.launch(Main.class,args);
    }
}
