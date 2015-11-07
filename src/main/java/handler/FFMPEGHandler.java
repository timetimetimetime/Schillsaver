package handler;

import controller.MainScreenController;
import javafx.application.Platform;
import misc.Job;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class FFMPEGHandler {
    /**
     * Encodes the specified file(s) using the settings in the specified
     * configuration handler.
     * @param job The Job being run.
     * @param selectedFiles The file(s) to encode.
     * @param controller The controller for the main screen.
     * @param configHandler The settings to use when encoding the handler(s).
     */
    public void encodeVideoToDisk(final Job job, List<File> selectedFiles, final MainScreenController controller, final ConfigHandler configHandler) {
        final ArchiveHandler archiveHandler = new ArchiveHandler();

        if(configHandler.getCombineAllFilesIntoSingleArchive()) {
            final File temp = archiveHandler.packFiles(job, selectedFiles, controller, configHandler);
            selectedFiles.clear();
            selectedFiles.add(temp);
        } else if(configHandler.getCombineIntoIndividualArchives()){
            for(int i = 0 ; i < selectedFiles.size() ; i++) {
                selectedFiles.set(i, archiveHandler.packFile(job, selectedFiles.get(i), controller, configHandler));
            }

            // Sort the array of files to ensure the smallest files
            // are encoded first.
            selectedFiles = greedySort(selectedFiles);
        }

        for(File f : selectedFiles) {
            FileHandler.padFile(f, configHandler);

            // Construct FFMPEG string:
            final StringBuilder stringBuilder = new StringBuilder();
            final Formatter formatter = new Formatter(stringBuilder, Locale.US);

            // Use the fully custom settings if they're enabled:
            if(configHandler.getUseFullyCustomFfmpegOptions() && !configHandler.getFullyCustomFfmpegEncodingOptions().isEmpty()) {
                formatter.format("\"%s\" %s",
                        configHandler.getFfmpegPath(),
                        configHandler.getFullyCustomFfmpegEncodingOptions());

                // Insert the input filename:
                final String inputFilename = "\"" + f.getAbsolutePath() + "\"";
                stringBuilder.replace(0, stringBuilder.length(), stringBuilder.toString().replace("FILE_INPUT", inputFilename));

                // Insert the output filename:
                final String outputFilename = "\"" + FilenameUtils.getFullPath(f.getAbsolutePath()) + FilenameUtils.getBaseName(f.getName()) + "." + configHandler.getEncodeFormat() + "\"";
                stringBuilder.replace(0, stringBuilder.length(), stringBuilder.toString().replace("FILE_OUTPUT", outputFilename));
            } else if (!configHandler.getUseFullyCustomFfmpegOptions()) {
                formatter.format("\"%s\" -f rawvideo -pix_fmt monob -s %dx%d -r %d -i \"%s\" -vf \"scale=iw*%d:-1\" -sws_flags neighbor -c:v %s -threads 8 -loglevel %s -y \"%s%s.%s\"",
                        configHandler.getFfmpegPath(),
                        (configHandler.getEncodedVideoWidth() / configHandler.getMacroBlockDimensions()),
                        (configHandler.getEncodedVideoHeight() / configHandler.getMacroBlockDimensions()),
                        configHandler.getEncodedFramerate(),
                        f.getAbsolutePath(),
                        configHandler.getMacroBlockDimensions(),
                        configHandler.getEncodingLibrary(),
                        configHandler.getFfmpegLogLevel(),
                        job.getOutputDirectory(),
                        FilenameUtils.getBaseName(f.getName()),
                        configHandler.getEncodeFormat());
            }

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.getView().getTextArea_output().appendText(stringBuilder.toString() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator());
                }
            });

            CommandHandler.runProgram(stringBuilder.toString(), controller);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.getView().getTextArea_output().appendText("ENCODING COMPLETED");
                    controller.getView().getTextArea_output().appendText(System.lineSeparator() + System.lineSeparator() + System.lineSeparator());
                }
            });

            // Delete leftovers:
            if(configHandler.getCombineAllFilesIntoSingleArchive()) {
                f.delete(); // This is just the archive, not the original handler.
            } else {
                if(configHandler.getDeleteSourceFileWhenEncoding()) {
                    f.delete(); // This is the original handler.
                }
            }

            if(configHandler.getDeleteSourceFileWhenDecoding()) {
               f = new File(f.getAbsolutePath().replace(configHandler.getDecodeFormat(), ""));
               f.delete();
            }
        }

        // Enable buttons after encoding:
        //controller.getView().enableButtons();
    }

    /**
     * Decodes the specified file(s) using the settings in the specified
     * configuration handler.
     * @param job The Job being run.
     * @param selectedFiles The file(s) to decode.
     * @param controller The controller for the main screen.
     * @param configHandler The settings to use when decoding the handler(s).
     */
    public void decodeVideo(final Job job, List<File> selectedFiles, final MainScreenController controller,  final ConfigHandler configHandler) {
        // Sort the array of files to ensure the smallest files
        // are decoded first.
        selectedFiles = greedySort(selectedFiles);

        for(final File f : selectedFiles) {
            // Construct FFMPEG string:
            final StringBuilder stringBuilder = new StringBuilder();
            final Formatter formatter = new Formatter(stringBuilder, Locale.US);

            // Use the fully custom settings if they're enabled:
            if(configHandler.getUseFullyCustomFfmpegOptions() && !configHandler.getFullyCustomFfmpegDecodingOptions().isEmpty()) {
                formatter.format("\"%s\" %s",
                        configHandler.getFfmpegPath(),
                        configHandler.getFullyCustomFfmpegEncodingOptions());

                // Insert the input filename:
                final String inputFilename = "\"" + f.getAbsolutePath() + "\"";
                stringBuilder.replace(0, stringBuilder.length(), stringBuilder.toString().replace("FILE_INPUT", inputFilename));

                // Insert the output filename:
                final String outputFilename = "\"" + FilenameUtils.getFullPath(f.getAbsolutePath()) + FilenameUtils.getBaseName(f.getName()) + "." + configHandler.getEncodeFormat() + "\"";
                stringBuilder.replace(0, stringBuilder.length(), stringBuilder.toString().replace("FILE_OUTPUT", outputFilename));
            } else if (!configHandler.getUseFullyCustomFfmpegOptions()) {
                formatter.format("\"%s\" -i \"%s\" -vf \"format=pix_fmts=monob,scale=iw*%f:-1\" -sws_flags area -loglevel %s -f rawvideo \"%s.%s\"",
                        configHandler.getFfmpegPath(),
                        f.getAbsolutePath(),
                        (1.0 / configHandler.getMacroBlockDimensions()),
                        configHandler.getFfmpegLogLevel(),
                        job.getOutputDirectory(),
                        configHandler.getDecodeFormat());
            }

            controller.getView().getTextArea_output().appendText(stringBuilder.toString() + System.lineSeparator() + System.lineSeparator() + System.lineSeparator());

            CommandHandler.runProgram(stringBuilder.toString(), controller);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.getView().getTextArea_output().appendText("DECODING COMPLETED");
                    controller.getView().getTextArea_output().appendText(System.lineSeparator() + System.lineSeparator() + System.lineSeparator());
                }
            });

            // Delete leftovers:
            if(configHandler.getDeleteSourceFileWhenDecoding()) {
               f.delete(); // This is just the archive, not the original handler.
            }
        }

        // Enable buttons after decoding:
        //controller.getView().enableButtons();
    }

    /**
     * To ensure that the smallest files are encoded first, the greedy
     * algorithm sorts the array by smallest filesize using mergesort.
     *
     * @param arr The file(s) to sort.
     * @return A sorted list of file(s) from smallest to largest filesize.
     */
    private List<File> greedySort(final List<File> arr) {
        // If the array has zero, or one, element, then it is already sorted.
        if(arr.size() == 0 || arr.size() == 1) {
            return arr;
        }

        // Split the array into two halves.
        int half = arr.size()/2;
        List<File> arrA = new ArrayList<>();
        List<File> arrB = new ArrayList<>();

        // Fill both halves with the values from arr.
        for(int i=0;i<half;i++) {
            arrA.add(arr.get(i));
        }

        for(int i=half;i<arr.size();i++) {
            arrB.add(i - half, arr.get(i));
        }

        // Recursively call the sort() method on both halves.
        arrA = greedySort(arrA);
        arrB = greedySort(arrB);

        // Combine the two sorted arrays while sorting.
        List<File> arrC = new ArrayList<>();

        int arrAIndex = 0, arrBIndex = 0, arrCIndex = 0;

        while(arrAIndex < arrA.size() && arrBIndex < arrB.size()) {
            if(arrA.size() < arrB.size()) {
                arrC.add(arrCIndex, arrA.get(arrAIndex));
                arrAIndex++;
            } else {
                arrC.add(arrCIndex, arrB.get(arrBIndex));
                arrBIndex++;
            }
            arrCIndex++;
        }

        // Because the above loop doesn't work for all elements we must
        // copy whatever elements remain into arrC.
        while(arrAIndex < arrA.size()) {
            arrC.add(arrCIndex, arrA.get(arrAIndex));
            arrCIndex++;
            arrAIndex++;
        }

        while(arrBIndex < arrB.size()) {
            arrC.add(arrCIndex, arrB.get(arrBIndex));
            arrCIndex++;
            arrBIndex++;
        }

        return arrC;
    }
}
