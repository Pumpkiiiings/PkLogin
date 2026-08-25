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
        
        // Paper's mirror is tried first because it is the one Paper asks plugins to
        // use, but neither of these artifacts resolves through it, and a library
        // that does not resolve stops the whole plugin from loading. Maven Central
        // itself is listed after it as the fallback that actually has them.
        //
        // m2.dv8tion.net only publishes JDA's stable releases, so the beta pinned
        // above has never been there; it is kept for whenever that version moves.
        resolver.addRepository(new RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-central/").build());
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        resolver.addRepository(new RemoteRepository.Builder("jda", "default", "https://m2.dv8tion.net/releases").build());
        
        classpathBuilder.addLibrary(resolver);
    }
}
