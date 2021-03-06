/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.ui.pdf;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.standalone.AutoFirmaUtil;
import es.gob.afirma.standalone.LookAndFeelManager;
import es.gob.afirma.standalone.ui.pdf.PdfLoader.PdfLoaderListener;
import es.gob.afirma.standalone.ui.pdf.SignPdfUiPanel.SignPdfUiPanelListener;

/** Di&aacute;logo para la obtenci&oacute;n de los datos de firma PDF Visible.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SignPdfDialog extends JDialog implements PdfLoaderListener, SignPdfUiPanelListener {

	private static final long serialVersionUID = -7987676963743094243L;

	private static final int PREFERRED_WIDTH = 500;
	private static final int PREFERRED_HEIGHT = 680;

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private final Frame parent;
	private final SignPdfDialogListener listener;

	private JScrollPane scrollPanel;
	private JPanel activePanel;
	SignPdfDialogListener getListener() {
		return this.listener;
	}

	private boolean isPdfSign;
	private byte[] pdfData;

	private final boolean signatureVisible;
	private final boolean stampVisible;

	/** Construye un di&aacute;logo para la obtenci&oacute;n de los datos de firma PDF Visible.
	 * @param parentFrame Marco padre para la modalidad.
	 * @param spdl Clase a la que notificar la obtencion de propiedades de la firma visible.
	 * @param signatureVisible Indica si se va a insertar una firma
	 * @param stampVisible Indica si se va a insertar una marca*/
	private SignPdfDialog(final Frame parentFrame,
						  final SignPdfDialogListener spdl,
						  final boolean signatureVisible,
						  final boolean stampVisible) {
		super(parentFrame);
		this.parent = parentFrame;
		this.listener = spdl;
		this.signatureVisible = signatureVisible;
		this.stampVisible = stampVisible;
		createUI();
	}

	private void createUI() {
		setTitle(SignPdfUiMessages.getString("SignPdfDialog.3")); //$NON-NLS-1$
		setIconImage(
			AutoFirmaUtil.getDefaultDialogsIcon()
		);
		getAccessibleContext().setAccessibleDescription(
			SignPdfUiMessages.getString("SignPdfDialog.2") //$NON-NLS-1$
		);
		setModalityType(ModalityType.TOOLKIT_MODAL);
		setLocationRelativeTo(this.parent);

		this.scrollPanel = new JScrollPane();
		this.scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(this.scrollPanel);

		addWindowListener(
			new java.awt.event.WindowAdapter() {
			    @Override
			    public void windowClosing(final java.awt.event.WindowEvent windowEvent) {
			    	positionCancelled();
			    }
			}
		);

	}

	/** Obtiene los par&aacute;metros adicionales de una firma visible PDF mediante
	 * un di&aacute;logo gr&aacute;fico.
	 * @param isSign <code>true</code> si el PDF de entrada ya contiene firmas electr&oacute;nicas previas,
	 *               <code>false</code> en caso contrario.
	 * @param pdf PDF al que aplicar la firma visible.
	 * @param parentFrame Marco padre para la modalidad.
	 * @param signatureVisible Indica si se va a insertar una firma
	 * @param stampVisible Indica si se va a insertar una marca
	 * @param spdl Clase a la que notificar la obtencion de propiedades de la firma visible. */
	public static void getVisibleSignatureExtraParams(final boolean isSign,
													  final byte[] pdf,
			                                          final Frame parentFrame,
			                                          final boolean signatureVisible,
			                                          final boolean stampVisible,
			                                          final SignPdfDialogListener spdl) {
		if (pdf == null || pdf.length < 3) {
			throw new IllegalArgumentException(
				"El PDF a aplicarle la firma visible no puede ser nulo ni vacio" //$NON-NLS-1$
			);
		}
		if (spdl == null) {
			throw new IllegalArgumentException(
				"La clase a la que notificar la obtencion de propiedades no puede ser nula" //$NON-NLS-1$
			);
		}

		final JDialog dialog = new SignPdfDialog(parentFrame, spdl, signatureVisible, stampVisible);
		dialog.setPreferredSize(getPreferredDimensionToSignatureDialog());
		final Point cp = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		dialog.setLocation(cp.x - (int) dialog.getPreferredSize().getWidth()/2, cp.y - (int) dialog.getPreferredSize().getHeight()/2);
		dialog.setResizable(false);

		PdfLoader.loadPdf(
			isSign,
			pdf,
			(PdfLoaderListener) dialog
		);
	}

	private List<BufferedImage> pages;
	private List<Dimension> pageSizes;

	@Override
	public void pdfLoaded(final boolean isSign,
			              final List<BufferedImage> listPages,
			              final List<Dimension> listPageSizes,
			              final byte[] pdf) {

		this.isPdfSign = isSign;
		this.pages = listPages;
		this.pageSizes = listPageSizes;
		this.pdfData = pdf;

		if(this.signatureVisible) {
			setPreferredSize(getPreferredDimensionToSignatureDialog());
			this.activePanel = new SignPdfUiPanel(
				this.isPdfSign,
				this.pages,
				this.pageSizes,
				this.pdfData,
				this
			);
			this.scrollPanel.setViewportView(this.activePanel);
		}
		else if(this.stampVisible) {
			setPreferredSize(getPreferredDimensionToStampDialog());
			setTitle(SignPdfUiMessages.getString("SignPdfUiStamp.0")); //$NON-NLS-1$
			this.activePanel = new SignPdfUiPanelStamp(
				this.pages,
				this.pageSizes,
				this.pdfData,
				this,
				new Properties()
			);
			this.scrollPanel.setViewportView(this.activePanel);
		}
		pack();

		setVisible(true);
	}

	private static Dimension getPreferredDimensionToSignatureDialog() {
		final double screenHeight = LookAndFeelManager.getScreenSize().getHeight();
		final Dimension dim = new Dimension(
			PREFERRED_WIDTH,
			(int) Math.min(PREFERRED_HEIGHT, screenHeight * 0.9)
		);
		return dim;
	}

	private static Dimension getPreferredDimensionToStampDialog() {
		final double screenHeight = LookAndFeelManager.getScreenSize().getHeight();
		final Dimension dim = new Dimension(
			PREFERRED_WIDTH,
			(int) Math.min(PREFERRED_HEIGHT + 60, screenHeight * 0.9)
		);
		return dim;
	}

	@Override
	public void pdfLoadedFailed(final Throwable cause) {
		LOGGER.severe("Error creando la previsualizacion del PDF: " + cause); //$NON-NLS-1$
		if (cause instanceof OutOfMemoryError) {
			AOUIFactory.showErrorMessage(
				this.parent,
				SignPdfUiMessages.getString("SignPdfDialog.4"), //$NON-NLS-1$
				SignPdfUiMessages.getString("SignPdfDialog.1"), //$NON-NLS-1$
				JOptionPane.ERROR_MESSAGE
			);
		}
		else {
			AOUIFactory.showErrorMessage(
				this.parent,
				SignPdfUiMessages.getString("SignPdfDialog.0"), //$NON-NLS-1$
				SignPdfUiMessages.getString("SignPdfDialog.1"), //$NON-NLS-1$
				JOptionPane.ERROR_MESSAGE
			);
		}
		setVisible(false);
		this.listener.propertiesCreated(new Properties());
		dispose();
	}

	@Override
	public void nextPanel(final Properties p, final BufferedImage im) {
		if(this.activePanel instanceof SignPdfUiPanel) {
			setPreferredSize(getPreferredDimensionToSignatureDialog());
			this.activePanel = new SignPdfUiPanelPreview(this, p, im);
			this.scrollPanel.setViewportView(this.activePanel);
			pack();
			((SignPdfUiPanelPreview) this.activePanel).requestFocusInWindow();
		}
		else if(this.activePanel instanceof SignPdfUiPanelPreview && this.stampVisible) {
			setPreferredSize(getPreferredDimensionToStampDialog());
			this.activePanel = new SignPdfUiPanelStamp(
				this.pages,
				this.pageSizes,
				this.pdfData,
				this,
				p
			);
			this.scrollPanel.setViewportView(this.activePanel);
			pack();
			((SignPdfUiPanelStamp) this.activePanel).requestFocusInWindow();
		}
		else {
			positionSelected(p);
		}
	}

	@Override
	public void positionSelected(final Properties extraParams) {
		setVisible(false);
		LOGGER.info("Propiedades establecidas mediante GUI: " + extraParams); //$NON-NLS-1$
		this.listener.propertiesCreated(extraParams);
		dispose();
	}

	@Override
	public void positionCancelled() {
		setVisible(false);
		this.listener.propertiesCreated(new Properties());
		dispose();
	}

	@Override
	public final Frame getParent() {
		return this.parent;
	}

	/** Define los requerimientos de las clases a las que se informa de que ya se cuenta
	 * con las propiedades de la firma visible PDF. */
	public static interface SignPdfDialogListener {

		/** Establece los par&aacute;metros adicionales de la firma visible PDF indicados por
		 * el usuario mediante el di&aacute;logo.
		 * @param extraParams Par&aacute;metros adicionales de la firma visible PDF, o un fichero
		 *                    de propiedades vac&iacute;o si al usuario cancel&oacute; la operaci&oacute;n
		 *                    o hubo un error por el que no pudieron recogerse. */
		void propertiesCreated(final Properties extraParams);
	}
}
