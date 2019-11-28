package com.virtuslab.gitmachete.test;

import com.virtuslab.gitmachete.root.GitFactoryModule;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.Repository;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteLoader;
import com.virtuslab.gitmachete.gitmachetejgit.GitMacheteLoaderFactory;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] argv) throws Exception {
        GitMacheteLoader loader = GitFactoryModule.getInjector().getInstance(GitMacheteLoaderFactory.class).create(Paths.get(System.getProperty("user.home"), "submodule-test"));
        Repository repo = null;

        try {
            repo = loader.getRepository();
        } catch(GitMacheteException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println(repo);

        System.out.println(repo.getSubmoduleRepositories());
    }
}
