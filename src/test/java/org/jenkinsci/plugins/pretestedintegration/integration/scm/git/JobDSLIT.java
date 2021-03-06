package org.jenkinsci.plugins.pretestedintegration.integration.scm.git;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by mads on 1/15/18.
 job("generated") {
    scm {
       git {
            remote {
                name("origin")
                url("some.repo.somewhere.git")
            }
            extensions {
                pretestedIntegration("ACCUMULATED","master","origin")
            }
        }
     }
    publishers {
        pretestedIntegration()
    }
 }
 */
public class JobDSLIT {

    List<Repository> repositories;

    String script = "job(\"generated\") {\n" +
            "    scm {\n" +
            "        git {\n" +
            "            remote {\n" +
            "                name(\"origin\")\n" +
            "                url(\"%REPO_URL\")\n" +
            "            }\n" +
            "            extensions {\n" +
            "                pretestedIntegration(\"ACCUMULATED\",\"master\",\"origin\")\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "  \tpublishers {\n" +
            "    \tpretestedIntegration()  \n" +
            "  \t}\n" +
            "}";

    @Rule
    public JenkinsRule jr = new JenkinsRule();

    @Test
    public void testGenerationOfJobDSLJob() throws Exception {
        /**
         * Step 1
         * Prepare a repository we can use. We create only a happy day scenario
         */
        String repoName = "test-repo";
        Repository repository = TestUtilsFactory.createValidRepository(repoName);
        repositories.add(repository);

        File workDir = new File(TestUtilsFactory.WORKDIR, repoName);

        Git.cloneRepository().setURI("file:///" + repository.getDirectory().getAbsolutePath()).setDirectory(workDir)
                .setBare(false)
                .setCloneAllBranches(true)
                .setNoCheckout(false)
                .call().close();

        /**
         * Step 2
         * Create a seed job to create the job we want to use
         */
        FreeStyleProject fp = jr.createFreeStyleProject("seed");
        ExecuteDslScripts ex = new ExecuteDslScripts();
        ex.setScriptText(script.replaceAll("%REPO_URL", "file://"+repository.getDirectory().getAbsolutePath()));
        fp.getBuildersList().add(ex);
        jr.buildAndAssertSuccess(fp);

        //Assert that our job was created
        assertNotNull(jr.getInstance().getItemByFullName("generated", FreeStyleProject.class));

        /**
         * Step 3
         * Create and execute the seed job assert success
         */
        FreeStyleBuild fb = jr.buildAndAssertSuccess(jr.getInstance().getItemByFullName("generated", FreeStyleProject.class));
        assertTrue(fb.getLog().contains("[PREINT] Done pushing changes"));
    }

    @Before
    public void setup() {
        repositories = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        for (Repository repo : repositories) {
            TestUtilsFactory.destroyRepo(repo);
        }
    }

}
