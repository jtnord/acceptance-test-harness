package org.jenkinsci.test.acceptance.utils;

import org.hamcrest.CustomTypeSafeMatcher;
import org.jenkinsci.test.acceptance.controller.ExistingJenkinsController;
import org.jenkinsci.test.acceptance.controller.JBossController;
import org.jenkinsci.test.acceptance.controller.JenkinsController;
import org.jenkinsci.test.acceptance.controller.LocalController;
import org.jenkinsci.test.acceptance.controller.RemoteJenkinsController;
import org.jenkinsci.test.acceptance.controller.TomcatController;
import org.jenkinsci.test.acceptance.controller.WinstoneController;
import org.jenkinsci.test.acceptance.controller.WinstoneDockerController;
import org.jenkinsci.test.acceptance.server.PooledJenkinsController;
import org.junit.Test;

import static org.jenkinsci.test.acceptance.utils.JenkinsUtilTest.RestartableController.restartable;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;


public class JenkinsUtilTest {

    @Test
    public void isRestartableJenkinsController() {
        assertThat(JenkinsController.class, is(not(restartable())));
        assertThat(ExistingJenkinsController.class, is(not(restartable())));
        assertThat(LocalController.class, is(not(restartable())));
        assertThat(JBossController.class, is(restartable()));
        assertThat(TomcatController.class, is(restartable()));
        assertThat(WinstoneController.class, is(restartable()));
        assertThat(WinstoneDockerController.class, is(restartable()));
        assertThat(PooledJenkinsController.class, is(not(restartable())));
        assertThat(RemoteJenkinsController.class, is(restartable()));
    }

    static class RestartableController extends CustomTypeSafeMatcher<Class<? extends JenkinsController>> {

        private RestartableController() {
            super("was not restartable");
        }

        static RestartableController restartable() {
            return new RestartableController();
        }

        @Override
        protected boolean matchesSafely(Class<? extends JenkinsController> item) {
            return JenkinsUtil.isRestartableJenkinsController(item);
        }

    }
}
