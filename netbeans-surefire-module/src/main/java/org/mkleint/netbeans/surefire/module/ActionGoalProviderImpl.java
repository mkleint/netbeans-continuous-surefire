/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.mkleint.netbeans.surefire.module;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Action;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.api.execute.RunUtils;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.netbeans.modules.maven.spi.actions.AbstractMavenActionsProvider;
import org.netbeans.modules.maven.spi.actions.MavenActionsProvider;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import static org.mkleint.netbeans.surefire.module.Bundle.*;

/**
 *
 * @author mkleint
 */
@ProjectServiceProvider(service=MavenActionsProvider.class, projectType="org-netbeans-modules-maven")
public class ActionGoalProviderImpl implements MavenActionsProvider {
    @StaticResource private static final String MAPPINGS = "org/mkleint/netbeans/surefire/module/actionMappings.xml";
    private static final String ACTION_ID = "continuous-surefire";
    
    private AbstractMavenActionsProvider delegate = new AbstractMavenActionsProvider() {

        protected @Override InputStream getActionDefinitionStream() {
            return ActionGoalProviderImpl.class.getClassLoader().getResourceAsStream(MAPPINGS);
        }
    };


    public @Override Set<String> getSupportedDefaultActions() {
        return new HashSet<String>(Arrays.asList(ACTION_ID));
    }
    
    
    @ActionID(id = "org.mkleint.netbeans.surefire.module.Surefire", category = "Project")
    @ActionRegistration(displayName = "#ACT_Surefire", lazy=false)
    @ActionReference(position = 1250, path = "Projects/org-netbeans-modules-maven/Actions")
    @Messages("ACT_Surefire=Start Continuous Test Execution")
    public static Action createReloadAction() {
        Action a = ProjectSensitiveActions.projectCommandAction(ACTION_ID, ACT_Surefire(), null);
        a.putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        return a;
    }


    public @Override synchronized boolean isActionEnable(String action, Project project, Lookup lookup) {
        if (ACTION_ID.equals(action)) {
            //TODO check if already running somehow?
            return RunUtils.isCompileOnSaveEnabled(project);
        }
        return false;
    }

    @Messages({
        "NbmActionGoalProvider.target_platform_not_running=You can only reload a module while running the application.",
        "NbmActionGoalProvider.no_app_found=No single open nbm-application project found with a dependency on this module."
    })
    public @Override RunConfig createConfigForDefaultAction(String actionName,
            Project project,
            Lookup lookup) {
        if (ACTION_ID.equals(actionName)) {
            return delegate.createConfigForDefaultAction(actionName, project, lookup);
        }
        return null;
    }

    public @Override NetbeansActionMapping getMappingForAction(String actionName,
            Project project) {
        if (ACTION_ID.equals(actionName)) {
            return delegate.getMappingForAction(actionName, project);
        }
        return null;
    }
}
