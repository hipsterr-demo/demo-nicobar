package com.hipsterr.demo.nicobar.helloworld;

import org.junit.Test;

import static org.junit.Assert.*;

public class GroovyModuleLoaderDemoTest {
    private GroovyModuleLoaderDemo groovyModuleLoaderDemo = new GroovyModuleLoaderDemo();
    @Test
    public void hello() throws Exception{
        groovyModuleLoaderDemo.hello();
    }
}