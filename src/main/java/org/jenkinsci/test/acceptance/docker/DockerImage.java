package org.jenkinsci.test.acceptance.docker;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.utils.process.CommandBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Container image, a template to launch virtual machines from.
 *
 * @author Kohsuke Kawaguchi
 */
public class DockerImage {

    public static final String DEFAULT_DOCKER_HOST = "127.0.0.1";
    public final String tag;
    DockerHostResolver dockerHostResolver;

    public DockerImage(String tag) {
        this.tag = tag;
        dockerHostResolver = new DockerHostResolver();
    }

    public <T extends DockerContainer> T start(Class<T> type, CommandBuilder options, CommandBuilder cmd,int portOffset) throws InterruptedException, IOException {
        DockerFixture f = type.getAnnotation(DockerFixture.class);
        return start(type,f.ports(),portOffset,f.bindIp(),options,cmd);
    }

    public <T extends DockerContainer> T start(Class<T> type, CommandBuilder options, CommandBuilder cmd) throws InterruptedException, IOException {
        DockerFixture f = type.getAnnotation(DockerFixture.class);
        return start(type,f.ports(),options,cmd);
    }

    public <T extends DockerContainer> T start(Class<T> type, int[] ports, CommandBuilder options, CommandBuilder cmd) throws InterruptedException, IOException {
        return start(type,ports,0, getDockerHost(),options,cmd);
    }
    /**
     * Starts a container from this image.
     */
    public <T extends DockerContainer> T start(Class<T> type, int[] ports,int localPortOffset, String ipAddress, CommandBuilder options, CommandBuilder cmd) throws InterruptedException, IOException {
        CommandBuilder docker = Docker.cmd("run");
        File cidFile = File.createTempFile("docker", "cid");
        cidFile.delete();
        docker.add("--cidfile="+cidFile);//strange behaviour in some docker version cidfile needs to come before

        for (int p : ports)
        {
            if(localPortOffset==0)//No manual offset, let docker figure out the best port for itself
            {
                docker.add("-p", ipAddress + "::" + p);
            }
            else {
                int localPort = localPortOffset + p;
                docker.add("-p", ipAddress + ":" + localPort + ":" + p);
            }
        }


        docker.add(options);
        docker.add(tag);
        docker.add(cmd);

        File tmplog = File.createTempFile("docker", "log"); // initially create a log file here

        Process p = docker.build()
                .redirectInput(new File("/dev/null"))
                .redirectErrorStream(true)
                .redirectOutput(tmplog)
                .start();

        // TODO: properly wait for either cidfile to appear or process to exit
        Thread.sleep(1000);

        if (cidFile.exists()) {
            try
            {
                p.exitValue();
                throw new IOException("docker died unexpectedly: "+docker+"\n"+FileUtils.readFileToString(tmplog));
            } catch (IllegalThreadStateException e)
            {
                //Docker is still running okay.
            }
            String cid;
            do {
                Thread.sleep(500);
                cid = FileUtils.readFileToString(cidFile);
            } while (cid==null || cid.length()==0);

            // rename the log file to match the container name
            File logfile = new File(cidFile+".log");
            tmplog.renameTo(logfile);

            System.out.printf("Launching Docker container `%s`: logfile is at %s\n", docker.toString(), logfile);

            try {
                T t = type.newInstance();
                t.init(cid,p,logfile);
                return t;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }

        } else {
            try {
                p.exitValue();
                throw new IOException("docker died unexpectedly: "+docker+"\n"+FileUtils.readFileToString(tmplog));
            } catch (IllegalThreadStateException e) {
                throw new IOException("docker didn't leave CID file yet still running. Huh?: "+docker+"\n"+FileUtils.readFileToString(tmplog));
            }
        }
    }

    // package for tests
    String getDockerHost() {
        final String dockerHostEnvironmentVariable = dockerHostResolver.getDockerHostEnvironmentVariable();
        return dockerHostEnvironmentVariable != null ? getIp(dockerHostEnvironmentVariable) : DEFAULT_DOCKER_HOST;
    }

    private String getIp(String uri) {
        final URI dockerHost = URI.create(uri);
        final String host = dockerHost.getHost();
        return host != null ? host : DEFAULT_DOCKER_HOST;
    }

    @Override
    public String toString() {
        return "DockerImage: "+tag;
    }

    static class DockerHostResolver {
        public String getDockerHostEnvironmentVariable() {
            return System.getenv("DOCKER_HOST");
        }
    }
}
