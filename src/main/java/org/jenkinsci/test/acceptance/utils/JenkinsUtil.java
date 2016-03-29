/*
 * The MIT License
 *
 * Copyright (c) 2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.test.acceptance.utils;

import java.io.IOException;

import javax.inject.Inject;

import org.jenkinsci.test.acceptance.controller.ExistingJenkinsController;
import org.jenkinsci.test.acceptance.controller.JBossController;
import org.jenkinsci.test.acceptance.controller.JenkinsController;
import org.jenkinsci.test.acceptance.controller.RemoteJenkinsController;
import org.jenkinsci.test.acceptance.controller.TomcatController;
import org.jenkinsci.test.acceptance.controller.WinstoneController;
import org.jenkinsci.test.acceptance.controller.WinstoneDockerController;
import org.jenkinsci.test.acceptance.po.Jenkins;
import org.jenkinsci.test.acceptance.server.PooledJenkinsController;

public class JenkinsUtil {

    /** Controller for the Jenkins under test */
    @Inject 
    private JenkinsController jenkinsController;

    /** The Jenkins under test */
    @Inject
    private Jenkins jenkins;

    /**
     * Asks Jenkins to restart via it's web UI (if possible) and falls back to stopping and starting Jenkins via the {@link JenkinsController}.
     * @throws IOException if Jenkins could not be restarted.
     */
    public void restart() throws IOException {
        if (canRestart()) {
            if (jenkins.canRestart()) {
                jenkins.restart();
            }
            else {
                jenkinsController.restart();
            }
        }
        throw new AssertionError("Test required Jenkins to restart but Jenkins is not restartable");
    }

    /**
     * Return <code>true</code> if Jenkins can restart itself or the {@link JenkinsController} is one which we know we can restart Jenkins.
     * This may return <code>false</code> for some derivatives of {@link JenkinsController} that we do not know about that can restart Jenkins
     * @return true if we can definitely restart the Jenkins instance under test, <code>false</code> if we are unsure or know we can not restart the Jenkins under test.
     */
    public boolean canRestart() {
        if (jenkins.canRestart()) {
            return true;
        }
        return isRestartableJenkinsController(jenkinsController.getClass());
    }


    static boolean isRestartableJenkinsController(Class<? extends JenkinsController> cls) {
        if (cls.equals(JBossController.class) ||
            cls.equals(TomcatController.class) ||
            cls.equals(WinstoneController.class) ||
            cls.equals(WinstoneDockerController.class) ||
            cls.equals(RemoteJenkinsController.class)) {
            return true;
        }
        if (cls.equals(ExistingJenkinsController.class)) {
            // it is existing so we don't know how to start it!
            return false;
        }
        if (cls.equals(PooledJenkinsController.class)) {
            // maybe maybe not - but no easy way to know for sure...
            return false;
        }
        // anything else
        return false;
    }
}
