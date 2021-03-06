package es.gob.afirma.signers.xades;

import javax.xml.crypto.dsig.DigestMethod;

/**
 * Constantes de firma XML o XAdES utilizadas en distintos puntos del m&oacute;dulo.
 */
public class XAdESConstants {

	/** Nombre del nodo de firma. */
	public static final String TAG_SIGNATURE = "Signature"; //$NON-NLS-1$

	/** Nombre del nodo de las referencias de firma. */
    static final String TAG_REFERENCE = "Reference"; //$NON-NLS-1$

    /** Nombre del nodo en el que se englobara&aacute;n las firmas cuando sea necesario. */
    static final String TAG_PARENT_NODE = "AFIRMA"; //$NON-NLS-1$

    /** Nombre del nodo "SignedInfo" con la informaci&oacute;n de firma. */
    static final String TAG_SIGNEDINFO = "SignedInfo"; //$NON-NLS-1$

	/** Prefijo que por defecto se usar&aacute; para referir al namespace de XMLdSig. */
    static final String DEFAULT_XML_SIGNATURE_PREFIX = "ds"; //$NON-NLS-1$

    /** Prefijo que por defecto se usar&aacute; para referir al namespace de XAdES. */
    static final String DEFAULT_XADES_SIGNATURE_PREFIX = "xades"; //$NON-NLS-1$

    /** Algoritmo de huella digital por defecto para las referencias XML. */
    static final String DEFAULT_DIGEST_METHOD = DigestMethod.SHA512;

    /** URI que define el espacio de nombres de XAdES v1.1.1. */
    static final String NAMESPACE_XADES_1_1_1 = "http://uri.etsi.org/01903/v1.1.1#";

    /** URI que define el espacio de nombres de XAdES v1.2.2. */
    static final String NAMESPACE_XADES_1_2_2 = "http://uri.etsi.org/01903/v1.2.2#";

    /** URI que define el espacio de nombres de XAdES v1.3.2. */
    static final String NAMESPACE_XADES_1_3_2 = "http://uri.etsi.org/01903/v1.3.2#"; //$NON-NLS-1$

    /** URI que define el espacio de nombres de XAdES v1.4.1. */
    static final String NAMESPACE_XADES_1_4_1 = "http://uri.etsi.org/01903/v1.4.1#"; //$NON-NLS-1$

    /** URI que define la versi&oacute;n por defecto de XAdES. */
    static final String DEFAULT_NAMESPACE_XADES = NAMESPACE_XADES_1_3_2;

    /** URI que define el tipo de propiedades firmadas de XAdES. */
    static final String REFERENCE_TYPE_SIGNED_PROPERTIES = "http://uri.etsi.org/01903#SignedProperties"; //$NON-NLS-1$

    /** URI que define una referencia de tipo MANIFEST. */
    static final String REFERENCE_TYPE_MANIFEST = "http://www.w3.org/2000/09/xmldsig#Manifest"; //$NON-NLS-1$

}
