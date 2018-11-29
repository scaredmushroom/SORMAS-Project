package de.symeda.sormas.ui.importer;

import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.symeda.sormas.api.I18nProperties;
import de.symeda.sormas.ui.utils.CssStyles;

@SuppressWarnings("serial")
public class CaseImportProgressLayout extends VerticalLayout {
	
	// Components
	private ProgressBar progressBar;
	private Label processedCasesLabel;
	private Label importedCasesLabel;
	private Label importErrorsLabel;
	private Label importSkipsLabel;
	private Button closeCancelButton;
	private HorizontalLayout infoLayout;
	private Label infoLabel;
	
	private ProgressBar progressCircle;
	private Image errorIcon;
	private Image successIcon;
	private Image warningIcon;
	private Component currentInfoComponent;
	
	private ClickListener cancelListener;
	
	// Counts
	private int processedCasesCount;
	private int importedCasesCount;
	private int importErrorsCount;
	private int importSkipsCount;
	private int totalCasesCount;
	
	public CaseImportProgressLayout(int totalCasesCount, Runnable cancelCallback) {
		this.totalCasesCount = totalCasesCount;
		
		setWidth(100, Unit.PERCENTAGE);
		setMargin(true);

		// Info text and icon/progress circle
		infoLayout = new HorizontalLayout();
		infoLayout.setWidth(100, Unit.PERCENTAGE);
		infoLayout.setSpacing(true);
		initializeInfoComponents();
		currentInfoComponent = progressCircle;
		infoLayout.addComponent(currentInfoComponent);
		infoLabel = new Label(String.format(I18nProperties.getText("importInfo"), totalCasesCount), ContentMode.HTML);
		infoLabel.setContentMode(ContentMode.HTML);
		infoLayout.addComponent(infoLabel);
		infoLayout.setExpandRatio(infoLabel, 1);
		
		addComponent(infoLayout);
		
		// Progress bar
		progressBar = new ProgressBar(0.0f);
		CssStyles.style(progressBar, CssStyles.VSPACE_TOP_3);
		addComponent(progressBar);
		progressBar.setWidth(100, Unit.PERCENTAGE);
		
		// Progress info
		HorizontalLayout progressInfoLayout = new HorizontalLayout();
		CssStyles.style(progressInfoLayout, CssStyles.VSPACE_TOP_5);
		progressInfoLayout.setSpacing(true);
		processedCasesLabel = new Label(String.format(I18nProperties.getText("importsProcessed"), 0, totalCasesCount));
		progressInfoLayout.addComponent(processedCasesLabel);
		importedCasesLabel = new Label(String.format(I18nProperties.getText("importImports"), 0));
		CssStyles.style(importedCasesLabel, CssStyles.LABEL_POSITIVE);
		progressInfoLayout.addComponent(importedCasesLabel);
		importErrorsLabel = new Label(String.format(I18nProperties.getText("importErrors"), 0));
		CssStyles.style(importErrorsLabel, CssStyles.LABEL_CRITICAL);
		progressInfoLayout.addComponent(importErrorsLabel);
		importSkipsLabel = new Label(String.format(I18nProperties.getText("importSkips"), 0));
		CssStyles.style(importSkipsLabel, CssStyles.LABEL_MINOR);
		progressInfoLayout.addComponent(importSkipsLabel);
		addComponent(progressInfoLayout);
		setComponentAlignment(progressInfoLayout, Alignment.TOP_RIGHT);
		
		// Cancel button
		closeCancelButton = new Button("Cancel");
		CssStyles.style(closeCancelButton, CssStyles.VSPACE_TOP_2);
		cancelListener = e -> {
			cancelCallback.run();
		};
		closeCancelButton.addClickListener(cancelListener);
		addComponent(closeCancelButton);
		setComponentAlignment(closeCancelButton, Alignment.MIDDLE_RIGHT);
	}
	
	private void initializeInfoComponents() {
		progressCircle = new ProgressBar();
		progressCircle.setIndeterminate(true);
		CssStyles.style(progressCircle, "v-progressbar-indeterminate-large");
		
		errorIcon = new Image(null, new ThemeResource("img/error-icon.png"));
		errorIcon.setHeight(35, Unit.PIXELS);
		errorIcon.setWidth(35, Unit.PIXELS);
		successIcon = new Image(null, new ThemeResource("img/success-icon.png"));
		successIcon.setHeight(35, Unit.PIXELS);
		successIcon.setWidth(35, Unit.PIXELS);
		warningIcon = new Image(null, new ThemeResource("img/warning-icon.png"));
		warningIcon.setHeight(35, Unit.PIXELS);
		warningIcon.setWidth(35, Unit.PIXELS);
	}
	
	public void updateProgress(CaseImportResult result) {		
		UI.getCurrent().access(new Runnable() {
			@Override
			public void run() {
				processedCasesCount++;
				if (result == CaseImportResult.SUCCESS) {
					importedCasesCount++;
					importedCasesLabel.setValue(String.format(I18nProperties.getText("importImports"), importedCasesCount));
				} else if (result == CaseImportResult.ERROR) {
					importErrorsCount++;
					importErrorsLabel.setValue(String.format(I18nProperties.getText("importErrors"), importErrorsCount));
				} else {
					importSkipsCount++;
					importSkipsLabel.setValue(String.format(I18nProperties.getText("importSkips"), importSkipsCount));
				}
				processedCasesLabel.setValue(String.format(I18nProperties.getText("importsProcessed"), processedCasesCount, totalCasesCount));
				progressBar.setValue((float) processedCasesCount / (float) totalCasesCount);
			}
		});
	}
	
	public void makeClosable(Runnable closeCallback) {
		closeCancelButton.setCaption("Close");
		closeCancelButton.removeClickListener(cancelListener);
		closeCancelButton.addClickListener(e -> {
			closeCallback.run();
		});
	}
	
	public void setInfoLabelText(String text) {
		infoLabel.setValue(text);
	}
	
	public void displayErrorIcon() {
		infoLayout.removeComponent(currentInfoComponent);
		currentInfoComponent = errorIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}
	
	public void displaySuccessIcon() {
		infoLayout.removeComponent(currentInfoComponent);
		currentInfoComponent = successIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}
	
	public void displayWarningIcon() {
		infoLayout.removeComponent(currentInfoComponent);
		currentInfoComponent = warningIcon;
		infoLayout.addComponentAsFirst(currentInfoComponent);
	}
	
}