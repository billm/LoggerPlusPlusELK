package com.nccgroup.loggerplusplus;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IExtensionStateListener;
import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.IGsonProvider;
import com.nccgroup.loggerplusplus.exports.ExportController;
import com.nccgroup.loggerplusplus.filterlibrary.FilterLibraryController;
import com.nccgroup.loggerplusplus.grepper.GrepperController;
import com.nccgroup.loggerplusplus.logentry.LogEntry;
import com.nccgroup.loggerplusplus.logging.LoggingController;
import com.nccgroup.loggerplusplus.logview.LogViewController;
import com.nccgroup.loggerplusplus.logview.processor.LogProcessor;
import com.nccgroup.loggerplusplus.preferences.PreferencesController;
import com.nccgroup.loggerplusplus.util.Globals;
import com.nccgroup.loggerplusplus.util.MoreHelp;
import com.nccgroup.loggerplusplus.util.userinterface.LoggerMenu;

import javax.swing.*;
import java.net.URL;
import java.util.List;

import static com.nccgroup.loggerplusplus.util.Globals.PREF_RESTRICT_TO_SCOPE;

/**
 * Created by corey on 07/09/17.
 */
public class LoggerPlusPlus implements IBurpExtender, IExtensionStateListener {
    public static LoggerPlusPlus instance;
    public static IBurpExtenderCallbacks callbacks;

    private final IGsonProvider gsonProvider;
    private LoggingController loggingController;
    private LogProcessor logProcessor;
    private ExportController exportController;
    private PreferencesController preferencesController;
    private LogViewController logViewController;
    private FilterLibraryController libraryController;
    private LoggerContextMenuFactory contextMenuFactory;
    private GrepperController grepperController;
    private MainViewController mainViewController;

    //UX
    private LoggerMenu loggerMenu;


    public LoggerPlusPlus(){
        this.gsonProvider = new DefaultGsonProvider();
    }

    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {

        //Fix Darcula's issue with JSpinner UI.
        try {
            Class spinnerUI = Class.forName("com.bulenkov.darcula.ui.DarculaSpinnerUI");
            UIManager.put("com.bulenkov.darcula.ui.DarculaSpinnerUI", spinnerUI);
            Class sliderUI = Class.forName("com.bulenkov.darcula.ui.DarculaSliderUI");
            UIManager.put("com.bulenkov.darcula.ui.DarculaSliderUI", sliderUI);
        } catch (ClassNotFoundException e) {
            //Darcula is not installed.
        }

        //Burp Specific
        LoggerPlusPlus.instance = this;
        LoggerPlusPlus.callbacks = callbacks;

        loggingController = new LoggingController(this);
        preferencesController = new PreferencesController(this, loggingController);
        exportController = new ExportController(this, preferencesController.getPreferences());
        libraryController = new FilterLibraryController(this, preferencesController);
        logViewController = new LogViewController(this, libraryController);
        logProcessor = new LogProcessor(this, logViewController.getLogTableController(), exportController);
        grepperController = new GrepperController(this, logViewController.getLogTableController(), preferencesController);
        contextMenuFactory = new LoggerContextMenuFactory(this);

        mainViewController = new MainViewController(this);


        callbacks.setExtensionName("Logger++");

        if(!callbacks.isExtensionBapp() && (boolean) preferencesController.getPreferences().getSetting(Globals.PREF_UPDATE_ON_STARTUP)){
            MoreHelp.checkForUpdate(false);
        }

        LoggerPlusPlus.callbacks.registerContextMenuFactory(contextMenuFactory);
        LoggerPlusPlus.callbacks.registerExtensionStateListener(LoggerPlusPlus.this);


        SwingUtilities.invokeLater(() -> {

            LoggerPlusPlus.callbacks.addSuiteTab(mainViewController);

            //Add menu item to Burp's frame menu.
            JFrame rootFrame = (JFrame) SwingUtilities.getWindowAncestor(mainViewController.getUiComponent());
            try{
                JMenuBar menuBar = rootFrame.getJMenuBar();
                loggerMenu = new LoggerMenu(LoggerPlusPlus.this);
                menuBar.add(loggerMenu, menuBar.getMenuCount() - 1);
            }catch (NullPointerException nPException){
                loggerMenu = null;
            }
        });

    }

    @Override
    public void extensionUnloaded() {
        if(loggerMenu != null && loggerMenu.getParent() != null){
            loggerMenu.getParent().remove(loggerMenu);
        }
        if(mainViewController.getPopOutWrapper().isPoppedOut()) {
            mainViewController.getPopOutWrapper().getPopoutFrame().dispose();
        }
        if(logViewController.getRequestViewerController().getRequestViewerPanel().isPoppedOut()) {
            logViewController.getRequestViewerController().getRequestViewerPanel().getPopoutFrame().dispose();
        }

        //Stop log processor executors and pending tasks.
        logProcessor.shutdown();

        //Null out static variables so not leftover.
        LoggerPlusPlus.instance = null;
        LoggerPlusPlus.callbacks = null;
    }

    public static boolean isUrlInScope(URL url){
        return (!(Boolean) instance.getPreferencesController().getPreferences().getSetting(PREF_RESTRICT_TO_SCOPE)
                || callbacks.isInScope(url));
    }


    public LogViewController getLogViewController() {
        return logViewController;
    }

    public IGsonProvider getGsonProvider() {
        return gsonProvider;
    }

    public GrepperController getGrepperController() {
        return grepperController;
    }

    public MainViewController getMainViewController() {
        return mainViewController;
    }

    public FilterLibraryController getLibraryController() {
        return libraryController;
    }

    public LoggingController getLoggingController() {
        return loggingController;
    }

    public PreferencesController getPreferencesController() {
        return preferencesController;
    }

    public LogProcessor getLogProcessor() {
        return logProcessor;
    }

    public LoggerMenu getLoggerMenu() {
        return loggerMenu;
    }

    public List<LogEntry> getLogEntries(){
        return logViewController.getLogTableController().getLogTableModel().getData();
    }

    public ExportController getExportController() {
        return exportController;
    }
}
