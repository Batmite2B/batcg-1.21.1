package bat.batcg;

import java.io.File;
import java.util.Arrays;

public class genModels {
    public static void main(String[] args) {
        File dir = new File("src/main/resources/assets/batcg/models/item/card/icon");
        Arrays.stream(dir.listFiles((d, name) -> name.endsWith(".json") && !name.equals("icon.json")))
                .sorted((a,b) -> a.getName().compareToIgnoreCase(b.getName()))
                .forEach(f -> {
                    String name = f.getName().replace(".json", "");
                    System.out.println("ctx.addModels(id(\"item/card/icon/" + name + "\"));");
                });
    }
}
