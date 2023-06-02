import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class DataEncoderDecoder {

    private static int bytesForTextLengthData = 4;
    private static int bitsInByte = 8;

    public static void main(String[] args) {
        // Specify the text file path and the output image path
        String textPath = "text.txt";
        String imagePath = "output.png";

        encode(textPath, imagePath);

        // Uncomment the following line if you want to decode the data from the image
        //decode(imagePath);
    }

    // Encode
    private static void encode(String textPath, String imagePath) {
        String text = getTextFromTextFile(textPath);
        byte[] compressedData = compressData(text);
        byte[] textLengthInBytes = getBytesFromInt(compressedData.length);

        int imageWidth = 1000; // Adjust as needed
        int imageHeight = 1000; // Adjust as needed

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
        byte[] imageInBytes = getBytesFromImage(image);

        try {
            encodeImage(imageInBytes, textLengthInBytes, 0);
            encodeImage(imageInBytes, compressedData, bytesForTextLengthData * bitsInByte);
        } catch (Exception exception) {
            System.out.println("Couldn't encode text in image. Error: " + exception);
            return;
        }

        saveImageToPath(image, new File(imagePath), "png");
        System.out.println("Successfully encoded text in: " + imagePath);
    }

    private static byte[] encodeImage(byte[] image, byte[] addition, int offset) {
        if (addition.length + offset > image.length) {
            throw new IllegalArgumentException("Image dimensions are not large enough to store the provided text.");
        }
        for (int i = 0; i < addition.length; i++) {
            int additionByte = addition[i];
            for (int bit = bitsInByte - 1; bit >= 0; --bit, offset++) {
                int b = (additionByte >>> bit) & 0x1;
                image[offset] = (byte) ((image[offset] & 0xFE) | b);
            }
        }
        return image;
    }

    // Decode
    private static void decode(String imagePath) {
        byte[] decodedData;
        try {
            BufferedImage imageFromPath = getImageFromPath(imagePath);
            byte[] imageInBytes = getBytesFromImage(imageFromPath);
            decodedData = decodeImage(imageInBytes);
            String uncompressedData = decompressData(decodedData);
            String outputFileName = "decoded_data.txt";
            saveDataToPath(uncompressedData, new File(outputFileName));
            System.out.println("Successfully extracted data to: " + outputFileName);
        } catch (Exception exception) {
            System.out.println("No data found in the image. Error: " + exception);
        }
    }

    private static byte[] decodeImage(byte[] image) {
        int length = 0;
        int offset = bytesForTextLengthData * bitsInByte;

        for (int i = 0; i < offset; i++) {
            length = (length << 1) | (image[i] & 0x1);
        }

        byte[] result = new byte[length];

        for (int b = 0; b < result.length; b++) {
            for (int i = 0; i < bitsInByte; i++, offset++) {
                result[b] = (byte) ((result[b] << 1) | (image[offset] & 0x1));
            }
        }
        return result;
    }

    // File I/O methods
    private static void saveImageToPath(BufferedImage image, File file, String extension) {
        try {
            file.delete();
            ImageIO.write(image, extension, file);
        } catch (Exception exception) {
            System.out.println("Image file could not be saved. Error: " + exception);
        }
    }

    private static void saveDataToPath(String data, File file) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(data);
            bufferedWriter.close();
        } catch (Exception exception) {
            System.out.println("Couldn't write data to file: " + exception);
        }
    }

    private static BufferedImage getImageFromPath(String path) {
        BufferedImage image = null;
        try {
            File file = new File(path);
            image = ImageIO.read(file);
        } catch (Exception exception) {
            System.out.println("Input image cannot be read. Error: " + exception);
        }
        return image;
    }

    private static String getTextFromTextFile(String textFile) {
        String text = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(textFile));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
            bufferedReader.close();
            text = stringBuilder.toString();
        } catch (Exception exception) {
            System.out.println("Couldn't read text from file. Error: " + exception);
        }
        return text;
    }

    // Compression methods
    private static byte[] compressData(String data) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream);
            deflaterOutputStream.write(data.getBytes());
            deflaterOutputStream.finish();
            deflaterOutputStream.close();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            System.out.println("Data compression failed. Error: " + exception);
        }
        return new byte[0];
    }

    private static String decompressData(byte[] compressedData) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
            InflaterInputStream inflaterInputStream = new InflaterInputStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inflaterInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inflaterInputStream.close();
            outputStream.close();
            return outputStream.toString();
        } catch (IOException exception) {
            System.out.println("Data decompression failed. Error: " + exception);
        }
        return "";
    }

    // Helpers
    private static byte[] getBytesFromImage(BufferedImage image) {
        WritableRaster raster = image.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        return buffer.getData();
    }

    private static byte[] getBytesFromInt(int integer) {
        return ByteBuffer.allocate(bytesForTextLengthData).putInt(integer).array();
    }
}
