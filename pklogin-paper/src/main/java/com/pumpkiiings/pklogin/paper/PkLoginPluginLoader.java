package com.pumpkiiings.pklogin.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public class PkLoginPluginLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        
        resolver.addDependency(new Dependency(new DefaultArtifact("net.dv8tion:JDA:5.0.0-beta.24"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.sun.mail:javax.mail:1.6.2"), null));
        
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.papermc.io/repository/maven-central/").build());
        resolver.addRepository(new RemoteRepository.Builder("jda", "default", "https://m2.dv8tion.net/releases").build());
        
        classpathBuilder.addLibrary(resolver);
    }
}
