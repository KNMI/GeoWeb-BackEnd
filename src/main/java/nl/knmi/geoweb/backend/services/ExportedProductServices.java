package nl.knmi.geoweb.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/exportedproducts")
public class ExportedProductServices {
    @Value("${geoweb.products.exportLocation}")
    private String productexportlocation;

    @RequestMapping(path = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<String> getExportedFiles() {
        List<String> exported = new ArrayList<>();
        Path path = Paths.get(productexportlocation);
        try {
            Files.list(Paths.get(productexportlocation))
                .sorted(Comparator.comparing(p -> {
                        String regex = ".*(\\d{14})\\.(\\w*)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher m = pattern.matcher(p.toFile().getName());
                        if (m.find()) {
                            return m.group(1) + "." + m.group(2);
                        }
                        return p.toFile().getName();
                    }
                ,Comparator.reverseOrder()))
                .forEach(p -> exported.add(p.getFileName().toString()));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return exported;
    }

    @RequestMapping(path = "/get",
        method = RequestMethod.GET)
    public void getExportedFile(@RequestParam(name = "file") String fileName, HttpServletResponse response) {
        File f = Paths.get(productexportlocation, fileName).toFile();
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
        String content = "";
        if (fileName.endsWith(".xml")) {
            wrapper.setContentType(MediaType.APPLICATION_XML_VALUE);
            wrapper.setCharacterEncoding("UTF-8");
        } else if (fileName.endsWith(".json")) {
            wrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
            wrapper.setCharacterEncoding("UTF-8");
        } else {
            wrapper.setContentType(MediaType.TEXT_PLAIN_VALUE);
            wrapper.setCharacterEncoding("UTF-8");
        }

        if (f.exists()) {
            try {
                content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
                response.setStatus(HttpStatus.OK.value());
            } catch (IOException e) {
                log.error(e.getMessage());
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        try {
            wrapper.getWriter().write(content);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
}
}
