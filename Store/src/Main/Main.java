package Main;

import DAO.AppPaths;
import Util.AppNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.nio.channels.*;
import java.nio.file.*;

public class Main extends Application {
    private FileChannel lockChannel;
    private FileLock lock;
    public static void main(String[] args) { launch(args); }
    @Override public void init() throws Exception {
        Path lockPath=AppPaths.daoFile("store.lock"); Files.createDirectories(lockPath.getParent());
        lockChannel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE);
        lock=lockChannel.tryLock();
        if(lock==null) { lockChannel.close(); throw new IllegalStateException("This store is already open in another application window."); }
    }
    @Override public void start(Stage stage) {
        new AppNavigator(stage).navigateToLogin();
        if(getParameters().getRaw().contains("--verify-install")) {
            Platform.runLater(() -> {
                try {
                    if(stage.getScene()==null)throw new IllegalStateException("No login scene");
                    stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
                    Files.writeString(AppPaths.daoFile("installation-check.txt"),"PASS: bundled runtime, SQLite, JavaFX and stylesheet");
                } catch(Exception e) { throw new RuntimeException(e); }
                finally { stage.close(); Platform.exit(); }
            });
        }
    }
    @Override public void stop() throws Exception { if(lock!=null)lock.release(); if(lockChannel!=null)lockChannel.close(); }
}
