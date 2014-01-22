/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mkleint.netbeans.surefire.module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.contrib.yenta.Yenta;

public class Installer extends Yenta {

    @Override
    protected Set<String> friends() {
        String[] s = new String[] {
            "org.netbeans.modules.maven",
            "org.netbeans.modules.maven.embedder",
        };
        return new HashSet<String>(Arrays.asList(s));
    }


}
