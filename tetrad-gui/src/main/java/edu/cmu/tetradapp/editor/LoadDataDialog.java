///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////
package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataModelList;
import edu.cmu.tetrad.util.JOptionUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Panel (to be put in a dialog) for letting the user choose how a data file
 * should be loaded.
 *
 * @author Joseph Ramsey
 */
final class LoadDataDialog extends JPanel {

    private File[] files;

    private File[] newFilesArr;

    private DataLoaderSettings dataLoaderSettings;

    private final transient DataModel[] dataModels;

    private final JTextArea summaryTextArea;

    public JTextArea fileTextArea;

    private int fileIndex;

    private JList fileList;

    private final DefaultListModel fileListModel;

    private Box filePreviewBox;

    private String previewBoxBorderTitle;

    private String summaryBoxBorderTitle;

    private final String defaulyPreviewBoxBorderTitle;

    private Box container;

    private Box settingsContainer;

    private Box previewContainer;

    private Box buttonsContainer;

    private Box fileListBox;

    private Box formatBox;

    private Box optionsBox;

    private Box summaryBox;

    private Box buttonsBox;

    private JButton addFileButton;

    private JButton step1Button;

    private JButton step2ForwardButton;

    private JButton step2BackwardButton;

    private JButton step3Button;

    private JButton finishButton;

    //================================CONSTRUCTOR=======================//
    public LoadDataDialog(File... files) {
        this.files = files;

        this.fileTextArea = new JTextArea();

        this.fileListModel = new DefaultListModel();

        this.fileIndex = 0;

        this.defaulyPreviewBoxBorderTitle = "Raw Data Preview (only first 20 rows): ";

        this.dataModels = new DataModel[files.length];

        this.summaryTextArea = new JTextArea();
    }

    //==============================PUBLIC METHODS=========================//
    public void showDataLoaderDialog() {
        // Overall container
        // contains data preview panel, loading params panel, and load button
        container = Box.createVerticalBox();
        // Must set the size of container, otherwise summaryBox gets shrinked
        container.setPreferredSize(new Dimension(900, 510));

        // Data loading params
        // The data loading params apply to all slected files
        // the users should know that the selected files should share these settings - Zhou
        dataLoaderSettings = new DataLoaderSettings(files);

        // Specify Format
        formatBox = dataLoaderSettings.specifyFormat();

        formatBox.setPreferredSize(new Dimension(900, 145));
        formatBox.setMaximumSize(new Dimension(900, 145));

        // Options settings
        optionsBox = dataLoaderSettings.selectOptions();

        optionsBox.setPreferredSize(new Dimension(900, 145));
        optionsBox.setMaximumSize(new Dimension(900, 145));

        // Contains file list and format/options
        settingsContainer = Box.createVerticalBox();

        settingsContainer.add(formatBox);
        settingsContainer.add(optionsBox);
        optionsBox.setVisible(false);

        // Add some padding between settingsContainer and preview container
        settingsContainer.add(Box.createVerticalStrut(10));

        // Add to overall container
        container.add(settingsContainer);

        // Preview container, contains file list and raw data preview
        previewContainer = Box.createHorizontalBox();
        previewContainer.setPreferredSize(new Dimension(900, 310));

        // Show all chosen files in a list
        for (File file : files) {
            // Add each file name to the list model
            fileListModel.addElement(file.getName());
        }

        fileList = new JList(fileListModel);
        // This mode specifies that only a single item can be selected at any point of time
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Default to select the first file and show its preview
        fileList.setSelectedIndex(0);

        // List listener
        // use an anonymous inner class to implement the event listener interface
        fileList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    fileIndex = fileList.getMinSelectionIndex();
                    if (fileIndex < 0) {
                        filePreviewBox.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(defaulyPreviewBoxBorderTitle), new EmptyBorder(5, 5, 5, 5)));
                        fileTextArea.setText("");
                    } else {
                        // Update the border title and show preview
                        previewBoxBorderTitle = defaulyPreviewBoxBorderTitle + files[fileIndex].getName();
                        filePreviewBox.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(previewBoxBorderTitle), new EmptyBorder(5, 5, 5, 5)));
                        setPreview(files[fileIndex], fileTextArea);
                    }
                }
            }
        });

        // Right click mouse on file name to show the close option
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (SwingUtilities.isRightMouseButton(e)) {
                    final int index = fileList.getSelectedIndex();

                    // Don't show the close option if selection is empty
                    if (index == -1) {
                        return;
                    }

                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem close = new JMenuItem("Remove this selected file from the loading list");
                    menu.add(close);

                    Point point = e.getPoint();
                    menu.show(fileList, point.x, point.y);

                    close.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // Can't remove if there's only one file left
                            if (files.length == 1) {
                                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                                        "You can't remove when there's only one file.");
                            } else {
                                // Close tab to show confirmation dialog
                                int selectedAction = JOptionPane.showConfirmDialog(JOptionUtils.centeringComp(),
                                        "Are you sure you want to remove this data file from the data loading list?",
                                        "Confirm", JOptionPane.OK_CANCEL_OPTION,
                                        JOptionPane.WARNING_MESSAGE);

                                if (selectedAction == JOptionPane.OK_OPTION) {
                                    // Remove the file from list model
                                    fileListModel.remove(index);

                                    System.out.println("Removed file of index = " + index + " from data loading list");

                                    // Also need to remove it from data structure
                                    files = ArrayUtils.remove(files, index);
                                }
                            }
                        }
                    });
                }
            }
        });

        // Put the list in a scrollable area
        JScrollPane fileListScrollPane = new JScrollPane(fileList);
        fileListScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        fileListBox = Box.createVerticalBox();
        fileListBox.setMinimumSize(new Dimension(305, 310));
        fileListBox.setMaximumSize(new Dimension(305, 310));
        fileListBox.add(fileListScrollPane);

        // Add gap between file list and add new file button
        fileListBox.add(Box.createVerticalStrut(10));

        // Add new files button
        addFileButton = new JButton("Add more files to the loading list ...");

        // Add file button listener
        addFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show file chooser
                JFileChooser fileChooser = new JFileChooser();
                String sessionSaveLocation = Preferences.userRoot().get("fileSaveLocation", "");
                fileChooser.setCurrentDirectory(new File(sessionSaveLocation));
                fileChooser.resetChoosableFileFilters();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                // Only allow to add one file at a time
                fileChooser.setMultiSelectionEnabled(true);
                // Customize dialog title bar text
                fileChooser.setDialogTitle("Add more files");
                // The second argument sets both the title for the dialog window and the label for the approve button
                int _ret = fileChooser.showDialog(container, "Choose");

                if (_ret == JFileChooser.CANCEL_OPTION) {
                    return;
                }

                // File array that contains only one file
                final File[] newFiles = fileChooser.getSelectedFiles();

                // Add the selected new file to existing files array
                // A list is a better choice but considering the old implementation
                // I'll keep using array - Zhou
                newFilesArr = new File[files.length + newFiles.length];
                System.arraycopy(files, 0, newFilesArr, 0, files.length);
                System.arraycopy(newFiles, 0, newFilesArr, files.length, newFiles.length);

                // Update the files array
                files = newFilesArr;

                // Add newly added files to the file list model
                for (File newFile : newFiles) {
                    // Add each file name to the list model
                    fileListModel.addElement(newFile.getName());
                }
            }
        });

        fileListBox.add(addFileButton);

        // Use a titled border with 5 px inside padding - Zhou
        String fileListBoxBorderTitle = "File to load";
        if (files.length > 1) {
            fileListBoxBorderTitle = "Files (right click to remove selected file)";
        }
        fileListBox.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(fileListBoxBorderTitle), new EmptyBorder(5, 5, 5, 5)));

        previewContainer.add(fileListBox);
        // Add some gap between file list and format box
        previewContainer.add(Box.createHorizontalStrut(10), 1);

        filePreviewBox = Box.createHorizontalBox();
        filePreviewBox.setMinimumSize(new Dimension(585, 315));
        filePreviewBox.setMaximumSize(new Dimension(585, 315));

        // Setup file text area.
        // We don't want the users to edit in the preview area - Zhou
        fileTextArea.setEditable(false);
        fileTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Set the default preview for the default selected file
        setPreview(files[0], fileTextArea);

        // Add the scrollable text area in a scroller
        final JScrollPane filePreviewScrollPane = new JScrollPane(fileTextArea);
        filePreviewBox.add(filePreviewScrollPane);

        // Show the default selected filename as preview border title
        previewBoxBorderTitle = defaulyPreviewBoxBorderTitle + files[0].getName();

        // Use a titled border with 5 px inside padding - Zhou
        filePreviewBox.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(previewBoxBorderTitle), new EmptyBorder(5, 5, 5, 5)));

        // Add to preview container
        previewContainer.add(filePreviewBox);

        // Add to overall container
        container.add(previewContainer);

        // Result summary
        summaryBox = Box.createHorizontalBox();

        summaryTextArea.setEditable(false);
        summaryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        final JScrollPane summaryScrollPane = new JScrollPane(summaryTextArea);
        summaryBox.add(summaryScrollPane);

        // Show the default selected filename as preview border title
        summaryBoxBorderTitle = "Step 3: Load & Review";

        // Use a titled border with 5 px inside padding - Zhou
        summaryBox.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(summaryBoxBorderTitle), new EmptyBorder(5, 5, 5, 5)));

        // Add to overall container
        container.add(summaryBox);
        // Hide by default
        summaryBox.setVisible(false);

        // Buttons
        // Step 1 button to specify format
        // You'll see Step 1 button only when you are ate step 2
        step1Button = new JButton("< Step 1: Specify Format");

        // Step 1 button listener
        step1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Hide the options settings and show format
                optionsBox.setVisible(false);
                formatBox.setVisible(true);

                // Show the step 2 forward button
                step2ForwardButton.setVisible(true);

                // Hide the step 2 backward button
                step2BackwardButton.setVisible(false);

                // Hide step 3 button
                step3Button.setVisible(false);

                // Hide finish button
                finishButton.setVisible(false);

                // Hide back button
                step1Button.setVisible(false);
            }
        });

        // Step 2 forward button to select options
        // You'll see Step 2 forward button only when you are ate step 1
        step2ForwardButton = new JButton("Step 2: Select Options >");

        // Step 2 forward button listener
        step2ForwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Hide format
                formatBox.setVisible(false);

                // Show options
                optionsBox.setVisible(true);

                // Show the step 1 button
                step1Button.setVisible(true);

                // Hide step 2 forward button
                step2ForwardButton.setVisible(false);

                // Show step 3 button
                step3Button.setVisible(true);

                // Hide finish button
                finishButton.setVisible(false);
            }
        });

        // Step 2 button to select options
        // You'll see Step 2 backward button only when you are ate step 3
        step2BackwardButton = new JButton("< Step 2: Select Options");

        // Step 2 backward button listener
        step2BackwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show file list
                fileListBox.setVisible(true);

                // Still hide format
                formatBox.setVisible(false);

                // Show options
                optionsBox.setVisible(true);

                // Show preview
                previewContainer.setVisible(true);

                // Hide summary
                summaryBox.setVisible(false);

                // Show the step 1 button
                step1Button.setVisible(true);

                // Hide step 2 backward button
                step2BackwardButton.setVisible(false);

                // Show step 3 button
                step3Button.setVisible(true);

                // Hide finish button
                finishButton.setVisible(false);
            }
        });

        // Step 3 button
        step3Button = new JButton("Step 3: Load & Review >");

        // Step 3 button listener
        step3Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Show result summary
                summaryBox.setVisible(true);

                // Hide all inside settingsContainer
                fileListBox.setVisible(false);
                formatBox.setVisible(false);
                optionsBox.setVisible(false);

                // Use previewContainer instead of previewBox
                // since the previewContainer also contains padding
                previewContainer.setVisible(false);

                // Hide step 1 button
                step1Button.setVisible(false);

                // Hide step 2 forward button
                step2ForwardButton.setVisible(false);

                // Show step 2 backward button
                step2BackwardButton.setVisible(true);

                // Hide step 3 button
                step3Button.setVisible(false);

                // Show finish button
                finishButton.setVisible(true);

                // Load data files and generate reviewing summary
                List<String> failedFiles = loadAndReview();

                // Determine if enable the finish button or not
                if (failedFiles.size() > 0) {
                    // Disable it
                    finishButton.setEnabled(false);
                } else {
                    // Load all files since all can be loaded successfully
                    loadAll();

                    // Enable it
                    finishButton.setEnabled(true);
                }
            }
        });

        // Finish button
        finishButton = new JButton("Finish");

        // Finish button listener
        finishButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Close the data loader dialog
                Window w = SwingUtilities.getWindowAncestor(finishButton);
                if (w != null) {
                    w.setVisible(false);
                }
            }
        });

        // Buttons container
        buttonsContainer = Box.createVerticalBox();

        // Add some padding between preview/summary and buttons container
        buttonsContainer.add(Box.createVerticalStrut(20));

        // Buttons box
        buttonsBox = Box.createHorizontalBox();
        buttonsBox.add(step1Button);
        // Don't use Box.createHorizontalStrut(20)
        buttonsBox.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonsBox.add(step2BackwardButton);
        buttonsBox.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonsBox.add(step2ForwardButton);
        buttonsBox.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonsBox.add(step3Button);
        buttonsBox.add(Box.createRigidArea(new Dimension(20, 0)));
        buttonsBox.add(finishButton);

        // Default to only show step forward button
        step1Button.setVisible(false);
        step2BackwardButton.setVisible(false);
        step3Button.setVisible(false);
        finishButton.setVisible(false);
        // Add to buttons container
        buttonsContainer.add(buttonsBox);

        // Add to overall container
        container.add(buttonsContainer);

        // Dialog without dialog buttons, because we use Load button to handle data loading
        // If we use the buttons come with JOptionPane.showOptionDialog(), the data loader dialog
        // will close automatically once we click one of the buttons.
        // We don't want to do this. We want to keep the data loader dialog in the backgroud of the
        // logging info dialog and close it if all files are loaded successfully.
        // Otherwise, still keep the data loader dialog there if fail to load any files - Zhou
        // Here no need to use the returned value since we are not handling the action buttons
        JOptionPane.showOptionDialog(JOptionUtils.centeringComp(), container,
                "Data File Loader", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{}, null);
    }

    /**
     * Load selected files and show the result summary dialog
     *
     * @return
     */
    private List<String> loadAndReview() {
        List<String> failedFiles = new ArrayList<String>();

        // Try to load each file and store the file name for failed loading
        for (int i = 0; i < files.length; i++) {
            DataModel dataModel = dataLoaderSettings.loadDataWithSettings(i, summaryTextArea, files);
            if (dataModel == null) {
                System.out.println("File index = " + i + " cannot be loaded due to error");

                // Add the file name to failed list
                failedFiles.add(files[i].getName());
            } else {
                System.out.println("File index = " + i + " can be loaded successfully, but wi will only be loaded once all files can be loaded together");
            }
        }

        return failedFiles;
    }

    /**
     * Add all files to model once all can be loaded successfully
     */
    private void loadAll() {
        // Try to load each file and store the file name for failed loading
        for (int i = 0; i < files.length; i++) {
            DataModel dataModel = dataLoaderSettings.loadDataWithSettings(i, summaryTextArea, files);

            addDataModel(dataModel, i, files[i].getName());

            System.out.println("File index = " + i + " has been loaded successfully");
        }
    }

    public DataModelList getDataModels() {
        DataModelList dataModelList = new DataModelList();

        for (DataModel dataModel : dataModels) {
            if (dataModel != null) {
                dataModelList.add(dataModel);
            }
        }

        return dataModelList;
    }

    /**
     * Set the file preview content
     *
     * @param file
     * @param textArea
     */
    private static void setPreview(File file, JTextArea textArea) {
        try {
            textArea.setText("");

            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                StringBuilder text = new StringBuilder();
                String line;
                int nLine = 0;

                // Only preview the first 20 rows
                // Will need to think about how to preview big file - Zhou
                while ((line = in.readLine()) != null) {
                    text.append(line.substring(0, line.length())).append("\n");

                    if (text.length() > 50000) {
                        textArea.append("(This file is too big to show the preview ...)\n");
                        break;
                    }

                    nLine++;
                    if (nLine >= 20) {
                        break;
                    }
                }

                textArea.append(text.toString());

                textArea.setCaretPosition(0);
            }
        } catch (IOException e) {
            textArea.append("<<<ERROR READING FILE>>>");
        }
    }

    private void addDataModel(DataModel dataModel, int index, String name) {
        if (dataModel == null) {
            throw new NullPointerException();
        }

        dataModel.setName(name);
        this.dataModels[index] = dataModel;
    }

}
