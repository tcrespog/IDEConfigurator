package es.gavab.ideconfigurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class IDEConfiguratorPlugin extends Plugin {

    private static final String NOT_ESCAPED_PATH_SYMBOL = "<<<\\$wsne>>>";
    private static final String ESCAPED_PATH_SYMBOL = "<<<\\$ws>>>";
    private static final String RESOURCES_ZIP = "resources.zip";
    public static final String PLUGIN_ID = "es.gavab.IDEConfigurator";
    private static final String FILES = "files.txt";
    private static final String ECLIPSE_INSTALLATION_DIR = "eclipse.install.dir";
    
    private static IDEConfiguratorPlugin plugin;
    protected boolean needsConfig;
    private Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);

    private static class AskingJob extends UIJob {

        boolean doConfig;

        public AskingJob(String name) {
            super(name);
        }

        public IStatus runInUIThread(IProgressMonitor monitor) {
            Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
            MessageBox mb = new MessageBox(shell, 196);
            StringBuilder sb = new StringBuilder();
            sb.append("IDEConfigurator has detected that the Eclipse installation directory has changed.");
            sb.append("\n");
            sb.append("This is normal if you have reused your workspace with another Eclipse instance.");
            sb.append("\n");
            sb.append("Do you want IDEConfigurator to re-configure your settings for DLTK Ruby and EclipseFP Haskell plug-ins?");
            sb.append("\n");
            sb.append("WARNING: preferences of these plug-ins changed by the user may be overriden.");

            mb.setText("Detected new Eclipse installation");
            mb.setMessage(sb.toString());
            int answer = mb.open();
            if (answer == 128) {
                this.doConfig = false;
            } else {
                this.doConfig = true;
            }
            return new Status(0, "es.gavab.IDEConfigurator", "IDEConfigurator");
        }
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);

        plugin = this;
    }

    public void initializeWorkspaceSettings() {
    	getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Do I have to initialize workspace settings?"));
        IPath path = getStateLocation();

        String eclipseInstallationDir = Platform.getInstallLocation().getURL().toString();

        needsConfiguration(eclipseInstallationDir);
        if (this.needsConfig) {
        	getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Yes I do"));
            File zipFile = new File(path.toFile(), "resources.zip");
            if (Platform.getOS().equals("win32")) {
                extractFileFromJarIfNecessary("/resources/windows/resources.zip", zipFile.getAbsolutePath());
            } else {
                extractFileFromJarIfNecessary("/resources/linux/resources.zip", zipFile.getAbsolutePath());
            }
            File pluginsStateDir = path.toFile().getParentFile();
            UnZip.extractFiles(zipFile.getAbsolutePath(), pluginsStateDir.getAbsolutePath());

            String cleanEclipseInstallationDir = eclipseInstallationDir.substring(eclipseInstallationDir.indexOf(':') + 1);
            File temp = new File(cleanEclipseInstallationDir);
            String eclipseParentDir = temp.getParent();

            if (Platform.getOS().equals("win32")) {
                extractFileFromJarIfNecessary("/resources/windows/files.txt", path.toOSString() + File.separator + "files.txt");
            } else {
                extractFileFromJarIfNecessary("/resources/linux/files.txt", path.toOSString() + File.separator + "files.txt");
            }            
//            extractFileFromJarIfNecessary("/files.txt", path.toOSString() + File.separator + "files.txt");
            File files = new File(path.toOSString(), "files.txt");
            try {
                BufferedReader br = new BufferedReader(new FileReader(files));
                String line = br.readLine();

                String userDirNotEscaped = eclipseParentDir;
                eclipseParentDir = eclipseParentDir.replaceAll("\\\\", "\\\\\\\\");
                eclipseParentDir = eclipseParentDir.replaceAll(":", "\\\\:");
                userDirNotEscaped = userDirNotEscaped.replace('\\', '/');
                userDirNotEscaped = userDirNotEscaped.replaceAll(":", "\\\\:");
                eclipseParentDir = Matcher.quoteReplacement(eclipseParentDir);
                userDirNotEscaped = Matcher.quoteReplacement(userDirNotEscaped);
                Map<String, String> matchRegExps = new HashMap<String, String>();
                matchRegExps.put("<<<\\$ws>>>", eclipseParentDir);
                matchRegExps.put("<<<\\$wsne>>>", userDirNotEscaped);
                while (line != null) {
                    substitutePathsInFile(new File(pluginsStateDir.getAbsolutePath(), line), matchRegExps);
                    line = br.readLine();
                }
                br.close();

                zipFile.delete();

                pluginPrefs.put(ECLIPSE_INSTALLATION_DIR, eclipseInstallationDir);
                pluginPrefs.flush();
                getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Workspace settings initialization done"));
            } catch (IOException e) {
                log(e);
            } catch (CoreException e) {
                log(e);
            } catch (BackingStoreException e) {
				log(e);
			}
        } else {
        	getLog().log(new Status(1, "es.gavab.IDEConfigurator", "No I don't"));
        }
    }

    private boolean needsConfiguration(String eclipseInstallDir) {
    	String previousDir = pluginPrefs.get(ECLIPSE_INSTALLATION_DIR, "");
        if ((previousDir == null) || ("".equals(previousDir))) {
            this.needsConfig = true;
        } else if (!eclipseInstallDir.equals(previousDir)) {
            AskingJob job = new AskingJob("IDEConfigurator");
            job.addJobChangeListener(new IJobChangeListener() {
                public void aboutToRun(IJobChangeEvent event) {
                }

                public void awake(IJobChangeEvent event) {
                }

                public void done(IJobChangeEvent event) {
                    IDEConfiguratorPlugin.AskingJob job = (IDEConfiguratorPlugin.AskingJob) event.getJob();
                    IDEConfiguratorPlugin.this.needsConfig = job.doConfig;
                }

                public void running(IJobChangeEvent event) {
                }

                public void scheduled(IJobChangeEvent event) {
                }

                public void sleeping(IJobChangeEvent event) {
                }
            });
            job.schedule();
            try {
                job.join();
            } catch (InterruptedException e) {
                log(e);
            }
        } else {
            this.needsConfig = false;
        }
        return this.needsConfig;
    }

    private void log(Exception e) {
        getLog().log(new Status(4, "es.gavab.IDEConfigurator", e.getMessage(), e));
    }

    private void substitutePathsInFile(File file, Map<String, String> regexps)
            throws CoreException {
        File newFile = new File(file.getParent(), file.getName() + ".substitute");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter wr = new PrintWriter(new FileWriter(newFile));
            String linea = br.readLine();
            while (linea != null) {
                for (Map.Entry<String, String> regexp : regexps.entrySet()) {
                    getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Replacing:" + linea));
                    linea = linea.replaceAll((String) regexp.getKey(), (String) regexp.getValue());
                    getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Replaced:" + linea));
                }
                wr.println(linea);
                linea = br.readLine();
            }
            br.close();
            wr.close();

            file.delete();
            newFile.renameTo(file);
        } catch (IOException e) {
            throw new CoreException(new Status(4, "es.gavab.IDEConfigurator", "Couldn't configure plugins", e));
        }
    }

    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static IDEConfiguratorPlugin getDefault() {
        return plugin;
    }

    private void extractFileFromJarIfNecessary(String srcFile, String destFile) {
        try {
        	getLog().log(new Status(1, "es.gavab.IDEConfigurator", "The source file:" + srcFile));
        	getLog().log(new Status(1, "es.gavab.IDEConfigurator", "The dest file:" + destFile));
            InputStream isCompiler = getClass().getResourceAsStream(srcFile);
            File compiler = new File(destFile);
            if (!compiler.exists()) {
                compiler.createNewFile();
            }
            OutputStream outCompiler = new FileOutputStream(compiler);

            byte[] info = new byte[2048];
            int leidos;
            while ((leidos = isCompiler.read(info)) != -1) {
                outCompiler.write(info, 0, leidos);
            }
            outCompiler.close();
            isCompiler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Unzip toolchains if they exist 
    public void extractTools() {
    	String[] zipNames = {"mingw.zip", "ruby.zip", "ghc.zip", "fpc.zip", "jre.zip"}; //Name convention for the zipped tools
    	File eclipseInstallDir = new File(Platform.getInstallLocation().getURL().getPath());
    	String eclipseInstallDir_path = eclipseInstallDir.getPath() + File.separator;
    	String eclipseRootDir_path = eclipseInstallDir.getParent() + File.separator; //Zipped tools must be inside eclipse installation directory
    	
    	ExecutorService executorService = Executors.newFixedThreadPool(zipNames.length); //One thread per unzipping task
    	
    	for (final String zipName : zipNames) {
    		final String zipFilePath = eclipseRootDir_path + zipName;
    		if (new File(zipFilePath).exists()) {
    		    final String folderFilePath; //Destination path where the folder will go in
    		    if (zipName.equals("jre.zip")) { //JRE locates inside eclipse folder
    			    folderFilePath = eclipseInstallDir_path;
    		    } else {
    			    folderFilePath = eclipseRootDir_path;
    		    }
    		
    			executorService.submit(new Runnable() {
					
					@Override
					public void run() {
						getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Starting extraction of " + zipName));
						UnZip.extractFiles(zipFilePath, folderFilePath);
						getLog().log(new Status(1, "es.gavab.IDEConfigurator", "Extraction of " + zipName + " completed"));
						new File(zipFilePath).delete(); //Delete the zip file after extraction
					}	
				});
    		}
    	}
    	
    }
}
