package es.gob.afirma.massive;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.MessageDigest;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.Test;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.massive.MassiveSignatureHelper.MassiveSignConfiguration;

/**
 * Clase para probar todas funciones de firma y multifirma disponibles para
 * la firma masiva program&aacute;tica.
 */
public class MassiveSignatureTest {

	private static final boolean MANUAL_DEBUG = true;

	private final static String path = new File("").getAbsolutePath(); //$NON-NLS-1$

    /**
     * Formatos de los cuales ejecutarse el test.
     * Campo 1: Identificador del formato
     * Campo 2: Nombre para la generacion del nombre de fichero
     * Campo 3: Extension de firma
     * Campo 4: Soporta contrafirma
     */
    private static final String[][] FORMATS = {
    	{AOSignConstants.SIGN_FORMAT_CMS, "CMS", "csig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_CADES, "CADES", "csig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XADES_DETACHED, "XADES_DETACHED", "xsig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPING, "XADES_ENVELOPING", "xsig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPED, "XADES_ENVELOPED", "xsig", "true", "false"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_DETACHED, "XMLDSIG_DETACHED", "xsig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPING, "XMLDSIG_ENVELOPING", "xsig", "true", "true"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPED, "XMLDSIG_ENVELOPED", "xsig", "true", "false"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_ODF, "ODF", "odt", "false", "false"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_OOXML, "OOXML", "docx", "false", "false"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	{AOSignConstants.SIGN_FORMAT_PDF, "PDF", "pdf", "false", "false"} //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    };

    private static final String[][] FORMATS_FILES = {
    	{AOSignConstants.SIGN_FORMAT_CMS, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_CADES, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XADES_DETACHED, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XADES_DETACHED, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPING, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPING, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPED, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_DETACHED, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_DETACHED, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPING, "bin"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPING, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPED, "xml"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_PDF, "pdf"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_ODF, "odt"},  //$NON-NLS-1$
    	{AOSignConstants.SIGN_FORMAT_OOXML, "docx"} //$NON-NLS-1$
    };

    private static final String[][] FORMATS_MODES = {
    	{AOSignConstants.SIGN_FORMAT_CMS, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_CMS, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_CADES, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_CADES, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XADES_DETACHED, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XADES_DETACHED, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPING, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPING, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XADES_ENVELOPED, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_DETACHED, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_DETACHED, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPING, AOSignConstants.SIGN_MODE_EXPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPING, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_XMLDSIG_ENVELOPED, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_PDF, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_ODF, AOSignConstants.SIGN_MODE_IMPLICIT},
    	{AOSignConstants.SIGN_FORMAT_OOXML, AOSignConstants.SIGN_MODE_IMPLICIT}
	};

    //private static final boolean[] ORIGINAL_FORMAT = new boolean[] {true, false};

    private static final String CERT_PATH = "ANF_PF_Activo.pfx"; //$NON-NLS-1$
    private static final String CERT_PASS = "12341234"; //$NON-NLS-1$
    private static final String CERT_ALIAS = "anf usuario activo"; //$NON-NLS-1$

//    private static final String CERT_PATH2 = "ANF_PJ_Activo.pfx"; //$NON-NLS-1$
//    private static final String CERT_PASS2 = "12341234"; //$NON-NLS-1$
//    private static final String CERT_ALIAS2 = "anf usuario activo"; //$NON-NLS-1$
//
//    private static final String CERT_PATH3 = "CAMERFIRMA_PF_SW_Clave_usuario_Activo.p12"; //$NON-NLS-1$
//    private static final String CERT_PASS3 = "1111"; //$NON-NLS-1$
//    private static final String CERT_ALIAS3 = "1"; //$NON-NLS-1$

    /**
     * Genera todo tipo de firmas y multifirmas masivas haciendo u
     * @throws Exception Cuando se produce cualquier error durante la ejecuci&oacute;n.
     */
    @SuppressWarnings("static-method")
	@Test
    public void pruebaTodasLasCombinacionesDeFirmaProgramatica() throws Exception {

    	final KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
    	ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
    	final PrivateKeyEntry pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));

    	final MassiveSignConfiguration config = new MassiveSignConfiguration(pke);
    	for (final String[] format : FORMATS) {
    		config.setDefaultFormat(format[0]);
//    		for (final boolean originalFormat : ORIGINAL_FORMAT) {
//    			config.setOriginalFormat(originalFormat);
    			for (final String[] mode : FORMATS_MODES) {
    				if (format[0].equals(mode[0])) {
    					config.setMode(mode[1]);
    					for (final String[] file : FORMATS_FILES) {
    						if (format[0].equals(file[0])) {

    							final String fullpath = getResourcePath(file[1]);
    							final FileInputStream fis = new FileInputStream(fullpath);
    							final byte[] data = AOUtil.getDataFromInputStream(fis);
    							fis.close();

    							final MassiveSignatureHelper massive = new MassiveSignatureHelper(config);
    							byte[] signature = massive.signFile(fullpath);
    							Assert.assertNotNull(signature);
    							saveData(signature, "Firma_file_" + file[1] + "_" + format[1] + "_" + /*originalFormat + "_" +*/ mode[1] + "." + format[2]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    							signature = massive.signData(data);
    							Assert.assertNotNull(signature);
    							saveData(signature, "Firma_data_" + file[1] + "_" + format[1] + "_" + /*originalFormat + "_" +*/ mode[1] + "." + format[2]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    							if (Boolean.parseBoolean(format[4]) && AOSignConstants.SIGN_MODE_IMPLICIT.equals(mode[1])) {
    								signature = massive.signHash(getDigestData(data));
    								Assert.assertNotNull(signature);
    								saveData(signature, "Firma_hash_" + file[1] + "_" + format[1] + "_" + /*originalFormat + "_" +*/ mode[1] + "." + format[2]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    							}
    						}
    					}
    				}
    			}
//    		}
    	}
    }

    private static String getResourcePath(final String filename) {
    	return MassiveSignatureTest.class.getResource("/" + filename).toString().replace("file:/", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static byte[] getDigestData(final byte[] data) throws Exception {
    	return MessageDigest.getInstance("SHA-1").digest(data); //$NON-NLS-1$
    }

    private static void saveData(final byte[] data, final String filename) {
    	if (MANUAL_DEBUG) {
    		try {
    			final java.io.FileOutputStream fos = new java.io.FileOutputStream(path + File.separator + filename);
    			fos.write(data);
    			try { fos.flush(); fos.close(); } catch (final Exception e) {
    				// Ignoramos los errores
    			}
    		} catch (final Exception e) {
    			Logger.getLogger("es.gob.afirma").severe( //$NON-NLS-1$
    					"Error al guardar el fichero " + path + File.separator + filename); //$NON-NLS-1$
    		}
    	}
    }
}
