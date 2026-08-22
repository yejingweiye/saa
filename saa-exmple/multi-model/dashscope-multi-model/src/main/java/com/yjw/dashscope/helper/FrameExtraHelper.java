package com.yjw.dashscope.helper;

import jakarta.annotation.PreDestroy;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bytedeco.javacpp.Loader.deleteDirectory;

@Component
// FrameExtraHelper: 使用 JavaCV (FFmpegFrameGrabber) 从视频中抽帧并保存为图片的辅助类
// 该类实现了 ApplicationRunner，理论上可以在应用启动时执行相关初始化逻辑（当前 run 方法为空）
public class FrameExtraHelper implements ApplicationRunner {

    // 私有构造函数，表明不应该通过 new 显式实例化（Spring 会通过反射创建实例）
    private FrameExtraHelper() {
    }

    // 缓存：保存抽取出的图片路径列表，线程安全的 ConcurrentHashMap
    private static final Map<String, List<String>> IMAGE_CACHE = new ConcurrentHashMap<>();

    // 视频文件位置（相对工程路径），用于从该视频中抽取帧
    private static final File videoUrl = new File(
            "saa-exmple/multi-model/dashscope-multi-model/src/main/resources/multimodel/video.mp4");

    // 抽帧后图片的存放目录（相对工程路径），若目录不存在会自动创建
    private static final String framePath = "saa-exmple/multi-model/dashscope-multi-model/src/main/resources/multimodel/frame/";

    // 日志对象
    private static final Logger log = LoggerFactory.getLogger(FrameExtraHelper.class);

    /**
     * 从 videoUrl 指定的视频中按帧抽取图片并保存到 framePath 目录下。
     * 抽取过程要点：
     * - 使用 FFmpegFrameGrabber 打开视频流并逐帧抓取
     * - 使用 Java2DFrameConverter 将 Frame 转为 BufferedImage
     * - 以 png 格式写文件并把生成的文件路径加入 IMAGE_CACHE
     *
     * 注意：该方法为静态工具方法，调用方需要保证视频文件存在且进程有写目录权限。
     */
    public static void getVideoPic() {
        List<String> strList = new ArrayList<>(); // 每一帧图片路径集合
        File dir = new File(framePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 离开 try 代码块的时候，会自动调用每个资源的 `.close()` 方法释放资源
        try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videoUrl.getPath()); // 视频
             Java2DFrameConverter converter = new Java2DFrameConverter()) {

            ff.start();
            ff.setFormat("mp4");

            // 获取视频的总帧数
            int length = ff.getLengthInFrames();

            Frame frame;

            // 从第1帧到最后一帧遍历（注意：根据 FFmpegFrameGrabber 的实现，索引和返回的 frame 可能有所差异）
            for (int i = 1; i < length; i++) {
                frame = ff.grabFrame();
                // 有些抓取到的是音频帧或空帧，判断 image 是否为 null
                if (frame.image == null) {
                    continue;
                }

                // 将帧转换为 BufferedImage 并写入文件
                BufferedImage image = converter.getBufferedImage(frame);
                String path = framePath + i + ".png"; // 第几帧图片
                File picFile = new File(path);
                ImageIO.write(image, "png", picFile); // BufferedImage 以格式png写入 File
                strList.add(path);
            }

            // 将生成的图片路径列表缓存起来，key 使用 "img"
            IMAGE_CACHE.put("img", strList);
            ff.stop();

        } catch (Exception e) {
            // 记录异常信息（只记录消息以兼容原代码），如果需要可以记录堆栈信息：log.error("...", e)
            log.error(e.getMessage());
        }
    }

    // 应用启动时会调用该方法（目前为空，保留以便将来在启动时触发抽帧或其他初始化）
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting to extract video frames");

        getVideoPic();

        log.info("Finished extracting video frames");
    }

    @PreDestroy
    public void destroy(){

        try {
            deleteDirectory(new File(framePath)); // 删除帧目录
        }catch (IOException e){
            log.error(e.getMessage());
        }

        log.info("Delete temporary files...");

    }

    public static List<String> getFrameList(){

        assert IMAGE_CACHE.get("img")!=null;
        return IMAGE_CACHE.get("img");
    }

    // 均匀采样选出 `numberOfImages` 张图片
  public static List<Media> createMediaList(int numberOfImages){
    List<String> imgList = IMAGE_CACHE.get("img");

    // 总帧数（图片数量）
    int totalFrames = imgList.size();
    // 计算间隔：将 totalFrames 均匀分成 numberOfImages 段，至少为 1（避免除以 0 或获取越界）
    int interval = Math.max(totalFrames / numberOfImages, 1);

    // 通过 IntStream 取出每隔 interval 的一张图片，构造 Media 列表并返回
    return IntStream.range(0, numberOfImages)
            // 取出第 i*interval 张图片路径
            .mapToObj(i -> imgList.get(i * interval))
            // 将图片路径包装成 Spring AI 的 Media（指定 MIME 类型并用 PathResource 引用文件）
            .map(image -> new org.springframework.ai.content.Media(
                    MimeType.valueOf("image/png"),
                    new PathResource(image)))
            .collect(Collectors.toList());
}




}
