/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mkleint.netbeans.surefire.module;

import java.io.File;
import org.netbeans.modules.maven.api.execute.ExecutionContext;
import org.netbeans.modules.maven.api.execute.LateBoundPrerequisitesChecker;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Utilities;

/**
 *
 * @author mkleint
 */
@ProjectServiceProvider(service={LateBoundPrerequisitesChecker.class}, projectType="org-netbeans-modules-maven")
public class Checker implements LateBoundPrerequisitesChecker {
    private static final String MAVENEXTCLASSPATH = "maven.ext.class.path";

    @Override
    public boolean checkRunConfig(RunConfig rc, ExecutionContext ec) {
        if ("continuous-surefire".equals(rc.getActionName()) && RunUtils.isCompileOnSaveEnabled(rc)) {
            rc.setProperty("surefire.server", "2999"); 
            File f = InstalledFileLocator.getDefault().locate("maven-nblib/netbeans-surefire-eventspy.jar", "org.mkleint.netbeans.surefire.module", false);
            assert f != null;
            String mavenPath = rc.getProperties().get(MAVENEXTCLASSPATH);
            if (mavenPath == null) {
                mavenPath = "";
            } else {
                mavenPath = mavenPath + (Utilities.isWindows() ? ";" : ":");
            }
            mavenPath = mavenPath + f.getAbsolutePath();
            rc.setProperty(MAVENEXTCLASSPATH, mavenPath);
        }
        return true;
    }

}
