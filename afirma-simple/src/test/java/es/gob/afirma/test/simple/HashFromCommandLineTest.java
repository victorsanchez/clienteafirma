package es.gob.afirma.test.simple;

import org.junit.Ignore;
import org.junit.Test;

import es.gob.afirma.standalone.SimpleAfirma;
import es.gob.afirma.standalone.ui.hash.HashUIHelper;

/** pruebas de huellas digitales desde l&iacute;nea de comandos.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class HashFromCommandLineTest {

	/** Prueba de la comprobaci&oacute;n de huellas digitales. */
	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testHashCheck() {
		HashUIHelper.checkHashUI("C:\\Users\\tomas\\AppData\\Local\\Temp\\sample-facturae.xml"); //$NON-NLS-1$
	}

	/** Prueba de comprobaci&oacute;n de huellas de directorio desde l&iacute;nea de comandos. */
	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testHashCheckDirectory() {
		SimpleAfirma.main(
				new String[] {
					"checkdigest", "-i", "/Users/user/Desktop/AfirmaConfigurator.hashfiles"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			);
	}

	/** Main para pruebas.
	 * @param args No se usa. */
	public static void main(final String[] args) {
		SimpleAfirma.main(
			new String[] {
				"createdigest", "-i", "/Users/user/Desktop/"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		);

	}

}
