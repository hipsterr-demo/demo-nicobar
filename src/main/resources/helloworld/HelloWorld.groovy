import java.util.concurrent.Callable;

public class HelloWorld implements Callable<String> {
    public String call() {
       return "Hello, World!";
    }
}
