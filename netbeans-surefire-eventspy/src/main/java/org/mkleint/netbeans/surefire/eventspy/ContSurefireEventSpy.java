package org.mkleint.netbeans.surefire.eventspy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginManagerException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mkleint
 */
public class ContSurefireEventSpy implements EventSpy {

    private boolean stopSurefire = false;
    private PlexusContainer container;
    private Logger logger;
    
    @Override
    public void init(Context context) throws Exception {
        Properties p = (Properties) context.getData().get("userProperties");
        if (p != null) {
            stopSurefire = p.containsKey("surefire.server");
        }
        logger.info("props=" + p);
        logger.info("stop=" + stopSurefire);
        container = (PlexusContainer) context.getData().get("plexus");
        logger.info("container=" + container);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        //this piece needs to run after NbEventSpy or the event logging will be confused.
        if (event instanceof ExecutionEvent) {
            ExecutionEvent ex = (ExecutionEvent) event;
        
            if (stopSurefire && ex.getMojoExecution() != null && (
                         ExecutionEvent.Type.MojoFailed.equals(ex.getType()) ||
                         ExecutionEvent.Type.MojoSucceeded.equals(ex.getType())) && "test".equals(ex.getMojoExecution().getGoal())) //TODO finetune goal condition
            {
                Thread serverThread = new Thread(getServerRunnable(ex.getMojoExecution(), ex.getSession()), "Netbeans Test Server Thread");
                stopSurefire = false;
                serverThread.start();
                synchronized (this) {
                    this.wait();
                }
            }
        }            
    }

    @Override
    public void close() throws Exception {
    }

    
    private Runnable getServerRunnable(final MojoExecution mojoExecution, final MavenSession session) {
        return new Runnable() {

            @Override
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(2999, 1);
                    logger.info("waiting for incoming connections on port 2999");
                    boolean cont = true;
                    final ExecutionEventCatapult eventCatapult = container.lookup(ExecutionEventCatapult.class);
                    final BuildPluginManager pluginManager = container.lookup(BuildPluginManager.class);
                    final MavenPluginManager mavenPluginManager = container.lookup(MavenPluginManager.class);
                    
                    while (cont) {
                        Socket s = ss.accept();
                        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        String line = br.readLine();
                        logger.info("processing " + line);
                        if (line == null || "exit".equals(line)) {
                            cont = false;
                            break;
 }
                        long start = System.currentTimeMillis();
                        
                        eventCatapult.fire(ExecutionEvent.Type.MojoStarted, session, mojoExecution);
                        try {
                            Mojo mojo = mavenPluginManager.getConfiguredMojo( Mojo.class, session, mojoExecution );
                            if (mojo instanceof ContextEnabled) {
                                ContextEnabled en = (ContextEnabled) mojo;
                                en.getPluginContext().clear();
                            }
                            
                            try {
                                pluginManager.executeMojo(session, mojoExecution);
                            } catch (MojoFailureException e) {
                                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
                            } catch (MojoExecutionException e) {
                                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
                            } catch (PluginConfigurationException e) {
                                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
                            } catch (PluginManagerException e) {
                                throw new LifecycleExecutionException(mojoExecution, session.getCurrentProject(), e);
                            }

                            eventCatapult.fire(ExecutionEvent.Type.MojoSucceeded, session, mojoExecution);
                        } catch (LifecycleExecutionException e) {
                            eventCatapult.fire(ExecutionEvent.Type.MojoFailed, session, mojoExecution, e);
                        } catch (PluginConfigurationException ex) {
                            java.util.logging.Logger.getLogger(ContSurefireEventSpy.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (PluginContainerException ex) {
                            java.util.logging.Logger.getLogger(ContSurefireEventSpy.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            s.close();
                            logger.info("has taken ms="  + ((System.currentTimeMillis() - start) ));
                        }
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(ContSurefireEventSpy.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ComponentLookupException ex) {
                    java.util.logging.Logger.getLogger(ContSurefireEventSpy.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    synchronized (ContSurefireEventSpy.this) {
                        ContSurefireEventSpy.this.notifyAll();
                    }
                    if (ss != null) {
                        try {
                            ss.close();
                        } catch (IOException ex) {
                            java.util.logging.Logger.getLogger(ContSurefireEventSpy.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        };
                
    }
}
