package com.hipsterr.demo.nicobar.helloworld;

import com.netflix.hystrix.Hystrix;
import com.netflix.nicobar.core.archive.JarScriptArchive;
import com.netflix.nicobar.core.execution.HystrixScriptModuleExecutor;
import com.netflix.nicobar.core.execution.ScriptModuleExecutable;
import com.netflix.nicobar.core.module.BaseScriptModuleListener;
import com.netflix.nicobar.core.module.ScriptModule;
import com.netflix.nicobar.core.module.ScriptModuleLoader;
import com.netflix.nicobar.core.module.ScriptModuleUtils;
import com.netflix.nicobar.core.persistence.ArchiveRepository;
import com.netflix.nicobar.core.persistence.ArchiveRepositoryPoller;
import com.netflix.nicobar.core.persistence.JarArchiveRepository;
import com.netflix.nicobar.core.plugin.ScriptCompilerPluginSpec;
import com.netflix.nicobar.core.utils.ClassPathUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class GroovyModuleLoaderDemo {
    private static final String GROOVY2_PLUGIN_ID = "groovy2";
    private static final String SCRIPT_MODULE_ID = "HelloWorld";
    private static final String ARCHIVE_JAR_NAME = "HelloWorld.jar";
    private static final String GROOVY2_COMPILER_PLUGIN_CLASS = "com.netflix.nicobar.groovy2.plugin.Groovy2CompilerPlugin";

    public void hello() throws Exception {
        ScriptCompilerPluginSpec spec = new ScriptCompilerPluginSpec.Builder(GROOVY2_PLUGIN_ID)
                .addRuntimeResource(getGroovyRuntime())
                .addRuntimeResource(getGroovyPluginLocation())
                .withPluginClassName(GROOVY2_COMPILER_PLUGIN_CLASS)
                .build();

        //创建监听器，用于创建，删除和更新 模块   此处使用默认实现
        BaseScriptModuleListener listener = new BaseScriptModuleListener() {
            public void moduleUpdated(ScriptModule newScriptModule, ScriptModule oldScriptModule) {
                System.out.printf("Received module update event. newModule: %s,  oldModule: %s%n", newScriptModule, oldScriptModule);
            }
        };

        ScriptModuleLoader scriptModuleLoader = new ScriptModuleLoader.Builder()
                .addPluginSpec(spec)
                .addListener(listener)
                .build();
        Path baseArchiveDir = Files.createTempDirectory(GroovyModuleLoaderDemo.class.getSimpleName());
        JarArchiveRepository repository = new JarArchiveRepository.Builder(baseArchiveDir).build();
        //部署jar文件
        deployTestArchive(repository);
        //创建poller 定时查看部署包是否更新
        ArchiveRepositoryPoller poller = new ArchiveRepositoryPoller.Builder(scriptModuleLoader).build();
        poller.addRepository(repository, 30, TimeUnit.SECONDS, true);

        //创建脚本调用信息
        ScriptModuleExecutable<String> executable = scriptModule -> {
            // the script doesn't necessarily have to implement any specific interfaces, but it does need to
            // be compilable to a class.
            ////非常有用的工具类，来查找模块下具有某接口的类型
            Class<?> callable = ScriptModuleUtils.findAssignableClass(scriptModule, Callable.class);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) callable.newInstance();
            String result = instance.call();
            return result;
        };

        String executeResult = executable.execute(scriptModuleLoader.getScriptModule(SCRIPT_MODULE_ID));
        System.out.println("Module(s) have been executed. Output: " + executeResult);


        // 下面为在Hystrix中执行
       /* HystrixScriptModuleExecutor<String> executor = new HystrixScriptModuleExecutor<>("TestModuleExecutor");
        //脚本真正执行
        List<String> results = executor.executeModules(Collections.singletonList(SCRIPT_MODULE_ID), executable, scriptModuleLoader);
        System.out.println("Module(s) have been executed. Output: " + results);

        // 释放Hystrix资源
        Hystrix.reset();*/
    }

    /**
     * 生成用于本次测试部署模块包 HelloWorld.jar 从项目resources文件夹下复制到临时目录文件中
     * HelloWorld.jar中的内容已经放在 resources/helloworld文件夹下，可以进行参考
     * @param repository
     * @throws IOException
     */
    private static void deployTestArchive (ArchiveRepository repository) throws IOException {
        InputStream archiveJarIs = GroovyModuleLoaderDemo.class.getClassLoader().getResourceAsStream(ARCHIVE_JAR_NAME);
        Path archiveToDeploy = Files.createTempFile(SCRIPT_MODULE_ID, ".jar");
        Files.copy(archiveJarIs, archiveToDeploy, StandardCopyOption.REPLACE_EXISTING);
        IOUtils.closeQuietly(archiveJarIs);
        //创建指定path下的部署包
        JarScriptArchive jarScriptArchive = new JarScriptArchive.Builder(archiveToDeploy).build();
        //将部署包插入到仓库中
        repository.insertArchive(jarScriptArchive);
    }

    /**
     * 获取groovy元信息路径（需要依赖 Nicobar-groovy2)
     * @return
     */
    private static Path getGroovyRuntime(){
        Path path = ClassPathUtils.findRootPathForResource("META-INF/groovy-release-info.properties", GroovyModuleLoaderDemo.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("couldn't find groovy-all.n.n.n.jar in the classpath.");
        }
        return path;
    }

    /**
     * 获取groovy2的编译插件
     * @return
     */
    private static Path getGroovyPluginLocation () {
        String resourceName = ClassPathUtils.classNameToResourceName(GROOVY2_COMPILER_PLUGIN_CLASS);
        Path path = ClassPathUtils.findRootPathForResource(resourceName, GroovyModuleLoaderDemo.class.getClassLoader());
        if (path == null) {
            throw new IllegalStateException("couldn't find groovy2 plugin jar in the classpath.");
        }
        return path;
    }

}