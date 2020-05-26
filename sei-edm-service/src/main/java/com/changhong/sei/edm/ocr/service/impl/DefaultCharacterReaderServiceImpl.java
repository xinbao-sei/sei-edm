package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.edm.common.util.ImageUtils;
import com.changhong.sei.edm.common.util.ZxingUtils;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import com.changhong.sei.util.FileUtils;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.QrcodeOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.QrcodeOCRResponse;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

// 导入对应产品模块的client
// 导入要请求接口对应的request response类

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 14:21
 */
@Component
public class DefaultCharacterReaderServiceImpl implements CharacterReaderService {

    /**
     * 识别条码匹配前缀
     */
    @Value("${sei.edm.ocr.match-prefix:sei}")
    private String matchStr;
    /**
     * tess data 安装目录
     */
    @Value("${sei.edm.ocr.tessdata-path:none}")
    private String tessDataPath;
    /**
     * 是否启用云识别
     */
    @Value("${sei.edm.ocr.cloud.enable:false}")
    private Boolean ocrCloudEnable;

    /**
     * 字符读取
     *
     * @param ocrType 识别类型
     * @param data    文件
     * @return 返回读取的内容
     */
    @Override
    public ResultData<String> read(DocumentType docType, OcrType ocrType, byte[] data) {
        String result = StringUtils.EMPTY;
        InputStream inputStream = new ByteArrayInputStream(data);

        // 条码前缀匹配内容
        String[] matchPrefix = StringUtils.split(matchStr.toLowerCase(), ",");

        switch (docType) {
            case Pdf:
                // 解码PDF中的条码信息.实质是将pdf转为图片后再解码
                try (PDDocument doc = PDDocument.load(inputStream)) {
                    result = doRecogonize(doc, 72, matchPrefix, ocrType);
                    if (StringUtils.isBlank(result)) {
                        result = doRecogonize(doc, 2 * 72, matchPrefix, ocrType);
                    }
                    if (StringUtils.isBlank(result)) {
                        result = doRecogonize(doc, 3 * 72, matchPrefix, ocrType);
                    }
                } catch (Exception e) {
                    LogUtil.error("the decode pdf may be not exit.");
                }
                break;
            case Image:
                BufferedImage image = null;
                try {
                    image = ImageIO.read(inputStream);
                    result = processImage(image, matchPrefix, ocrType);
                } catch (Exception e) {
                    LogUtil.error("the decode image may be not exit.", e);
//                } finally {
//                    if (image != null) {
//                        image.flush();
//                    }
                }

                break;
            default:
                //throw new IllegalArgumentException("不支持的文件类型。");
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!checkBarcode(result, matchPrefix, ocrType)) {
            result = StringUtils.EMPTY;
        }

        if (OcrType.InvoiceQr == ocrType) {
            String[] arr;
            if (StringUtils.isNotBlank(result)) {
                arr = result.split("[,]");
                if (arr.length >= 7) {
                    StringBuilder s = new StringBuilder();
                    // 发票代码
                    s.append("{\"code\":\"").append(arr[2]).append("\",");
                    // 发票号码
                    s.append("\"number\":\"").append(arr[3]).append("\",");
                    // 发票种类 01-增值税专用发票 04-增值税普通发票 10-增值税电子普通发票
                    if ("01".equals(arr[1])) {
                        s.append("\"category\":\"增值税专用发票\",");
                    } else if ("04".equals(arr[1])) {
                        s.append("\"category\":\"增值税普通发票\",");
                    } else if ("10".equals(arr[1])) {
                        s.append("\"category\":\"增值税电子普通发票\",");
                    }
                    // 开票金额(不含税) arr[4]
                    s.append("\"amount\":").append(arr[4]).append(",");
                    // 开票日期 arr[5]
                    s.append("\"date\":\"").append(arr[5]).append("\",");
                    // 校验码
                    s.append("\"checkCode\":\"").append(arr[6]).append("\",");
                    if (arr.length > 7) {
                        //随机码
                        s.append("\"random\":\"").append(arr[7]).append("\"}");
                    }
                    result = s.toString();
                }
            }
        }

        return ResultData.success(result);
    }

    /**
     * OCR识别
     */
    private String partImgOcr(BufferedImage bimg, final String[] matchPrefix) {
        String result = StringUtils.EMPTY;
        if (StringUtils.isBlank(tessDataPath) || StringUtils.equalsIgnoreCase("none", tessDataPath)) {
            return result;
        }

        try {
            //开始识别
            ITesseract instance = new Tesseract();
            //语言：英文
            instance.setLanguage("eng");
            //Tesseract的文字库
//        instance.setDatapath("/opt/tesseract-4.1.0/tessdata");
            //本地调试
//        instance.setDatapath("D:\\Program Files\\Tesseract-OCR\\tessdata");
            instance.setDatapath(tessDataPath);
            int pageIteratorLevel = ITessAPI.TessPageIteratorLevel.RIL_WORD;
            List<Word> words = instance.getWords(bimg, pageIteratorLevel);
            if (!CollectionUtils.isEmpty(words)) {
                //处理：去掉开头结尾的空白字符
                Word word = words.parallelStream().filter(i -> {
                    if (Objects.nonNull(i)) {
                        String text = i.getText();
                        if (StringUtils.isNotBlank(text)) {
                            text = text.trim();

                            // 如果已“NO.”开头，去掉“NO.”
                            if (text.toUpperCase().startsWith("NO.")) {
                                text = text.substring(3);
                            }
                            // 匹配指定前缀
                            return StringUtils.startsWithAny(text.toLowerCase(), matchPrefix);
                        }
                    }
                    return false;
                }).findAny().orElse(null);

                if (Objects.nonNull(word)) {
                    result = word.getText();
                    // 如果已“NO.”开头，去掉“NO.”
                    if (result.toUpperCase().startsWith("NO.")) {
                        result = result.substring(3);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String doRecogonize(PDDocument doc, float dpi, String[] matchPrefix, OcrType ocrType) {
        BufferedImage image = null;
        PDFRenderer renderer = new PDFRenderer(doc);
        try {
            image = renderer.renderImageWithDPI(0, dpi, ImageType.GRAY);

            return processImage(image, matchPrefix, ocrType);
        } catch (Exception e) {
            LogUtil.error("the decode pdf may be not exit.", e);
            return null;
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

    private String processImage(BufferedImage image, String[] matchPrefix, OcrType ocrType) {
        BufferedImage image1, image2;
        String result;
        if (Objects.equals(OcrType.Barcode, ocrType)) {
            result = ZxingUtils.processImageBarcode(image, matchPrefix);
        } else {
            result = ZxingUtils.processImageQr(image, matchPrefix);
        }
        if (!checkBarcode(result, matchPrefix, ocrType)) {
            int height = image.getHeight();
            int width = image.getWidth();
            // 剪切右上角
            image1 = image.getSubimage(width / 2, 0, width / 2, height / 4);
            // 指定识别右上角
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image1, matchPrefix);
            } else {
                result = ZxingUtils.processImageQr(image1, matchPrefix);
            }

            // ocr识别
            if (!checkBarcode(result, matchPrefix, ocrType)) {
                //条码识别失败，进行ocr识别
                result = partImgOcr(image1, matchPrefix);
            }
            image1 = null;
        }

        // 识别失败，原图片旋转90度再次识别
        if (!checkBarcode(result, matchPrefix, ocrType)) {
            // 旋转90度
            image1 = ImageUtils.rotate(image, 90);
            int height = image1.getHeight();
            int width = image1.getWidth();

            // 剪切右上角
            image2 = image1.getSubimage(width / 2, 0, width / 2, height / 4);
//                        BufferedImage image2 = image.getSubimage(0, 0, width, height / 2);
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image2, matchPrefix);
            } else {
                result = ZxingUtils.processImageQr(image2, matchPrefix);
            }

            // ocr识别
            if (!checkBarcode(result, matchPrefix, ocrType)) {
                //条码识别失败，进行ocr识别
                result = partImgOcr(image2, matchPrefix);
            }
            image1 = null;
            image2 = null;
        }

        // 识别失败，原图片旋转180度再次识别
        if (!checkBarcode(result, matchPrefix, ocrType)) {
            // 旋转180度
            image1 = ImageUtils.rotate(image, 180);
            int height = image1.getHeight();
            int width = image1.getWidth();

            // 剪切右上角
            image2 = image1.getSubimage(width / 2, 0, width / 2, height / 4);
//                        BufferedImage image2 = image.getSubimage(0, 0, width, height / 2);
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image2, matchPrefix);
            } else {
                result = ZxingUtils.processImageQr(image2, matchPrefix);
            }

            // ocr识别
            if (!checkBarcode(result, matchPrefix, ocrType)) {
                //条码识别失败，进行ocr识别
                result = partImgOcr(image2, matchPrefix);
            }
            image1 = null;
            image2 = null;
        }

        // 识别失败，调用云服务识别
        if (!checkBarcode(result, matchPrefix, ocrType)
                && ocrCloudEnable) {
            result = tencentQrcodeOCRApi(image);
        }

        return result;
    }

    private String tencentQrcodeOCRApi(BufferedImage image) {
        String result = "";
        InputStream is = null;
        try {
            is = ImageUtils.image2InputStream(image, "");
            result = tencentQrcodeOCRApi(FileUtils.stream2Str(is));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private String tencentQrcodeOCRApi(String image) {
        String result = StringUtils.EMPTY;
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey
            Credential cred = new Credential("AKIDCAwSlnhAVTHuHUp960CgusStT0a0LVs0 ", "tVyMZZJoIekZlhfMn8mBKEsaDZeYsxmR");

            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ocr.tencentcloudapi.com");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            OcrClient client = new OcrClient(cred, "ap-guangzhou", clientProfile);

            StringBuilder params = new StringBuilder();
            params.append("{\"ImageBase64\":\"data:image/png;base64,").append(image).append("\"}");
            QrcodeOCRRequest req = QrcodeOCRRequest.fromJsonString(params.toString(), QrcodeOCRRequest.class);
            params = null;
            // 通过client对象调用想要访问的接口，需要传入请求对象
            QrcodeOCRResponse resp = client.QrcodeOCR(req);
            // 输出json格式的字符串回包
            LogUtil.debug("调用腾讯识别服务响应结果: {}", QrcodeOCRRequest.toJsonString(resp));
            if (ArrayUtils.isNotEmpty(resp.getCodeResults())) {
                result = resp.getCodeResults()[0].getUrl();
            }
        } catch (TencentCloudSDKException e) {
            LogUtil.error("调用腾讯识别服务异常", e);
        }
        return result;
    }

    /**
     * 检查识别内容
     */
    private boolean checkBarcode(String data, String[] matchPrefix, OcrType ocrType) {
        if (StringUtils.isBlank(data)) {
            return false;
        }
        // 是否是条码
        if (OcrType.Barcode == ocrType) {
            // 前缀匹配
            if (matchPrefix != null && matchPrefix.length > 0
                    && !StringUtils.startsWithAny(data.toLowerCase(), matchPrefix)) {
                return false;
            }
        }
        return true;
    }
}
