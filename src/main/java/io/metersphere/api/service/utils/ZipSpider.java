package io.metersphere.api.service.utils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.jmeter.utils.FileUtils;
import io.metersphere.node.util.LogUtil;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.jmeter.config.CSVDataSet;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPFileArg;
import org.apache.jorphan.collections.HashTree;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ZipSpider {

    //根据网址返回网页源代码
    public static String getHtmlFromUrl(String url, String encoding) {
        StringBuffer html = new StringBuffer();
        InputStreamReader isr = null;
        BufferedReader buf = null;
        String str = null;
        try {
            URL urlObj = new URL(url);
            URLConnection con = urlObj.openConnection();
            isr = new InputStreamReader(con.getInputStream(), encoding);
            buf = new BufferedReader(isr);
            while ((str = buf.readLine()) != null) {
                html.append(str + "\n");
            }
            //sop(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    buf.close();
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return html.toString();
    }

    //根据网址下载网络文件到硬盘，包括图片，Gif图，以及压缩包
    public static void download(String url, String path) {
        File file = null;
        FileOutputStream fos = null;
        String downloadName = url.substring(url.lastIndexOf("/") + 1);
        HttpURLConnection httpCon = null;
        URLConnection con = null;
        URL urlObj = null;
        InputStream in = null;
        byte[] size = new byte[1024];
        int num = 0;
        try {
            file = new File(path + downloadName);
            fos = new FileOutputStream(file);
            if (url.startsWith("http")) {
                urlObj = new URL(url);
                con = urlObj.openConnection();
                httpCon = (HttpURLConnection) con;
                in = httpCon.getInputStream();
                while ((num = in.read(size)) != -1) {
                    for (int i = 0; i < num; i++)
                        fos.write(size[i]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //解压本地文件至目的文件路径
    public static void unzip(String fromFile, String toFile) {
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(fromFile)); BufferedInputStream bin = new BufferedInputStream(zin);) {
            String Parent = toFile;
            File fout = null;
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null && !entry.isDirectory()) {
                fout = new File(Parent, entry.getName());
                if (!fout.exists()) {
                    (new File(fout.getParent())).mkdirs();
                }
                FileOutputStream out = new FileOutputStream(fout);
                BufferedOutputStream Bout = new BufferedOutputStream(out);
                int b;
                while ((b = bin.read()) != -1) {
                    Bout.write(b);
                }
                Bout.close();
                out.close();
                LogUtil.info(fout + "解压成功");
            }
            bin.close();
            zin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //从总目录下解压文件里所有的压缩包至目的文件路径
    public static void unzipFromLoc(String filePath) throws Exception {
        File file = new File(filePath);
        File[] list = file.listFiles();
        String from = "";
        String to = "E:\\myDownload\\unzipFileFromWeb\\";
        for (File f : list) {
            boolean bool = f.isFile();
            if (bool) {
                from = f.getAbsolutePath();
                from = from.replace("\\", "\\\\");
                sop(from);
                unzip(from, to);
            }
        }
    }

    public static void sop(Object obj) {
        LogUtil.info(obj);
    }

    public static void seperate(char c) {
        for (int x = 0; x < 100; x++) {
            System.out.print(c);
        }
        sop("");
    }

    @SuppressWarnings("finally")
    public static File downloadFile(String urlPath, String downloadDir, String json) {
        File file = null;
        BufferedInputStream bin = null;
        OutputStream out = null;
        try {
            URL url = new URL(urlPath);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;// http的连接类
            //String contentType = httpURLConnection.getContentType();//请求类型,可用来过滤请求，
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(1000 * 5);//设置超时
            httpURLConnection.setRequestMethod("POST");//设置请求方式，默认是GET
            httpURLConnection.setRequestProperty("Charset", "UTF-8");// 设置字符编码
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpURLConnection.setInstanceFollowRedirects(false);
            byte[] writebytes = json.getBytes();
            // 设置文件长度
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
            OutputStream outwritestream = httpURLConnection.getOutputStream();
            outwritestream.write(json.getBytes());
            outwritestream.flush();
            outwritestream.close();

            httpURLConnection.connect();// 打开连接
            bin = new BufferedInputStream(httpURLConnection.getInputStream());

            String fileName = httpURLConnection.getHeaderField("Content-Disposition");
            fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename") + 10, fileName.length() - 1), "UTF-8");
            String path = downloadDir + File.separatorChar + fileName;// 指定存放位置
            file = new File(path);
            // 校验文件夹目录是否存在，不存在就创建一个目录
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            out = new FileOutputStream(file);
            int size = 0;

            byte[] b = new byte[2048];
            //把输入流的文件读取到字节数据b中，然后输出到指定目录的文件
            while ((size = bin.read(b)) != -1) {
                out.write(b, 0, size);
            }
            // 关闭资源
            bin.close();
            out.close();
            LogUtil.info("文件下载成功！");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LogUtil.info("文件下载失败！");
        } finally {
            try {
                bin.close();
                out.close();
            } catch (Exception e) {

            }
            return file;
        }
    }

    public static void getFiles(HashTree tree, List<BodyFile> files) {
        for (Object key : tree.keySet()) {
            HashTree node = tree.get(key);
            if (key instanceof HTTPSamplerProxy) {
                HTTPSamplerProxy source = (HTTPSamplerProxy) key;
                if (source != null && source.getHTTPFiles().length > 0) {
                    for (HTTPFileArg arg : source.getHTTPFiles()) {
                        BodyFile file = new BodyFile();
                        file.setId(arg.getParamName());
                        file.setName(arg.getPath());
                        files.add(file);
                    }
                }
            } else if (key instanceof CSVDataSet) {
                CSVDataSet source = (CSVDataSet) key;
                if (source != null && source.getFilename() != null) {
                    BodyFile file = new BodyFile();
                    file.setId(source.getFilename());
                    file.setName(source.getFilename());
                    files.add(file);
                }
            }
            if (node != null) {
                getFiles(node, files);
            }
        }
    }

    public static void downloadFiles(String uri, HashTree hashTree) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        List<BodyFile> files = new ArrayList<>();
        ZipSpider.getFiles(hashTree, files);
        if (CollectionUtils.isNotEmpty(files)) {
            try {
                URL urlObject = new URL(uri);
                String url = urlObject.getProtocol() + "://" + urlObject.getHost() + (urlObject.getPort() > 0 ? ":" + urlObject.getPort() : "") + "/api/jmeter/download/files";
                LogUtil.info("开始同步下载附件：" + url);
                ZipSpider.downloadFile(url, FileUtils.BODY_FILE_DIR, JSON.toJSONString(files));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] get(String uri, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        ResponseEntity<byte[]> result = restTemplate.getForEntity(uri, byte[].class);
        return result.getBody();
    }

    @SuppressWarnings("finally")
    public static File downloadFile(String urlPath, String downloadDir) {
        File file = null;
        try {
            URL url = new URL(urlPath);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;// http的连接类
            //String contentType = httpURLConnection.getContentType();//请求类型,可用来过滤请求，
            httpURLConnection.setConnectTimeout(1000 * 5);//设置超时
            httpURLConnection.setRequestMethod("GET");//设置请求方式，默认是GET
            httpURLConnection.setRequestProperty("Charset", "UTF-8");// 设置字符编码
            httpURLConnection.connect();// 打开连接

            BufferedInputStream bin = new BufferedInputStream(httpURLConnection.getInputStream());
            String fileName = httpURLConnection.getHeaderField("Content-Disposition");
            fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename") + 10, fileName.length() - 1), "UTF-8");
            String path = downloadDir + File.separatorChar + fileName;// 指定存放位置
            file = new File(path);
            // 校验文件夹目录是否存在，不存在就创建一个目录
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            OutputStream out = new FileOutputStream(file);
            int size = 0;

            byte[] b = new byte[2048];
            //把输入流的文件读取到字节数据b中，然后输出到指定目录的文件
            while ((size = bin.read(b)) != -1) {
                out.write(b, 0, size);
            }
            // 关闭资源
            bin.close();
            out.close();
            LogUtil.info("文件下载成功！");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LogUtil.info("文件下载失败！");
        } finally {
            return file;
        }
    }
}
