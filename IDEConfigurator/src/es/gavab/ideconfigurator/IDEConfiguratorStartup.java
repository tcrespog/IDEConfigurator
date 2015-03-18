package es.gavab.ideconfigurator;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;

public class IDEConfiguratorStartup
        implements IStartup {

    public void earlyStartup() {
    	IDEConfiguratorPlugin.getDefault().getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Starting up"));
        IDEConfiguratorPlugin.getDefault().initializeWorkspaceSettings();
    }
}