package nl.knmi.geoweb.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exportedproducts")
public class ExportedProductServices {
    @Value("${geoweb.products.exportLocation}")
    private String productexportlocation;

	@RequestMapping(path = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<String> getExportedFiles()  {
	    List<String> exported = new ArrayList<>();
        Path path = Paths.get(productexportlocation);
        try {
            Files.list(Paths.get(productexportlocation))
            .sorted(Comparator.comparing(p -> {
                String regex = ".*(\\d{14})\\..*";
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(p.toFile().getName());
                if (m.find()) {
                    return m.group(1);
                }
                return p.toFile().getName() ;
            }
            ))
            .forEach(p-> exported.add(p.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exported;
    }

    @RequestMapping(path="/get",
        method = RequestMethod.GET)
    public String getExportedFile(@RequestParam(name="file")  String fileName, HttpServletResponse response){
	    File f = Paths.get(productexportlocation, fileName).toFile();
	    String content="";
	    if (fileName.endsWith(".xml")) {
            response.addHeader("Content-Type", "application/xml");
        } else if (fileName.endsWith(".json")) {
            response.addHeader("Content-Type", "application/json");
        } else {

        }
        if (f.exists()) {
            try {
                content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
            } catch (IOException e) {
                e.printStackTrace();
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        return content;
    }
}
