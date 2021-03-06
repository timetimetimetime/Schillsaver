package misc;

import com.valkryst.VMVC.Settings;
import lombok.Getter;
import lombok.NonNull;

import java.awt.Dimension;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Job implements Serializable {
    private static final long serialVersionUID = 0;

    /** The name of the Job. */
    @Getter private String name;
    /** The output directory. */
    @Getter private String outputDirectory;
    /** The file(s) belonging to the Job.*/
    @Getter private List<File> files;
    /** Whether the Job is an Encode Job or a Decode Job. */
    @Getter private boolean isEncodeJob;

    /**
     * Constructs a new Job.
     *
     * @param builder
     *          The builder
     *
     * @throws NullPointerException
     *         If the builder is null.
     */
    Job(final @NonNull JobBuilder builder) {
        name = builder.getName();
        outputDirectory = builder.getOutputDirectory();
        files = builder.getFiles();
        isEncodeJob = builder.isEncodeJob();
    }

    /**
     * Zips all of the Job's files into a single archive.
     *
     * @param settings
     *          The settings.
     *
     * @return
     *         The zip file.
     *
     * @throws IOException
     *         If an I/O error occurs.
     *
     * @throws NullPointerException
     *         If the settings is null.
     */
    public File zipFiles(final @NonNull Settings settings) throws IOException {
        final File zipFile = new File(name + ".zip");

        final var fos = new FileOutputStream(zipFile);
        final var zos = new ZipOutputStream(fos);

        byte[] buffer = new byte[32_768];

        for (final File file : files) {
            if (! file.isDirectory()) {
                final var entry = new ZipEntry(file.getName());
                final var fis = new FileInputStream(file);

                zos.putNextEntry(entry);

                int read;
                while ((read = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, read);
                }

                zos.closeEntry();
                fis.close();
            }
        }

        zos.close();
        fos.close();

        return padFile(zipFile, settings);
    }

    /**
     * Pads the specified handler to ensure it contains enough data to
     * have an exact number of frames. If there are, for example,
     * 1401 bytes and the frame size is 1400 bytes, then ffmpeg will
     * display an error about not having a full frame worth of bytes.
     *
     * @param file
     *          The file.
     *
     * @param settings
     *          The settings.
     *
     * @throws NullPointerException
     *         If the file or settings is null.
     */
    private static File padFile(final @NonNull File file, final @NonNull Settings settings) {
        try (
            final var outputStream = new FileOutputStream(file, true);
        ) {
            final var frameDimension = FrameDimension.valueOf(settings.getStringSetting("Encoding Frame Dimensions"));
            final Dimension blockSize = BlockSize.valueOf(settings.getStringSetting("Encoding Block Size")).getBlockSize();

            int bytesPerFrame = frameDimension.getWidth() * frameDimension.getHeight();
            bytesPerFrame /= blockSize.width * blockSize.height;
            bytesPerFrame /= 8;

            final int numberOfBytesToPad = bytesPerFrame - (int) (file.length() % bytesPerFrame);

            outputStream.write(new byte[numberOfBytesToPad]);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }

        return file;
    }

    /**
     * Retrieves the summed pre-padding pre-zipped filesize of the job's
     * files.
     *
     * @return
     *          The pre-padding pre-zipped filesize of the job.
     */
    public long getFileSize() {
        long fileSize = 0;

        for (final File file : files) {
            fileSize += file.length();
        }

        return fileSize;
    }
}
