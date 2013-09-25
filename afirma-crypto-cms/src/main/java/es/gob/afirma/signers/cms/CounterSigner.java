/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.signers.cms;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.CounterSignTarget;
import es.gob.afirma.signers.pkcs7.AOAlgorithmID;
import es.gob.afirma.signers.pkcs7.P7ContentSignerParameters;
import es.gob.afirma.signers.pkcs7.SigUtils;

/** Clase que implementa la contrafirma digital PKCS#7/CMS SignedData La
 * implementaci&oacute;n del c&oacute;digo ha seguido los pasos necesarios para
 * crear un mensaje SignedData de BouncyCastle: <a
 * href="http://www.bouncycastle.org/">www.bouncycastle.org</a> pero con la
 * peculiaridad de que es una Contrafirma. */
final class CounterSigner {

    private int actualIndex = 0;
    private ASN1Set signedAttr2;
    private Map<String, byte[]> atrib2 = new HashMap<String, byte[]>();
    private Map<String, byte[]> uatrib2 = new HashMap<String, byte[]>();

    /** Constructor de la clase. Se crea una contrafirma a partir de los datos
     * del firmante, el archivo que se firma y del archivo que contiene las
     * firmas.<br>
     * @param parameters
     *        par&aacute;metros necesarios que contienen tanto la firma del
     *        archivo a firmar como los datos del firmante.
     * @param data
     *        Archivo que contiene las firmas.
     * @param targetType
     *        Lo que se quiere firmar. Puede ser el &aacute;rbol completo,
     *        las hojas, un nodo determinado o unos determinados firmantes.
     * @param targets
     *        Nodos objetivos a firmar.
     * @param key Clave privada a usar para firmar.
     * @param dataType
     *        Identifica el tipo del contenido a firmar.
     * @param atri
     *        Atributo firmado que agregar a la firma.
     * @param uatri
     *        Atributo no firmado que agregar a la firma.
     * @return El archivo de firmas con la nueva firma.
     * @throws java.io.IOException
     *         Si ocurre alg&uacute;n problema leyendo o escribiendo los
     *         datos
     * @throws java.security.NoSuchAlgorithmException
     *         Si no se soporta alguno de los algoritmos de firma o huella
     *         digital
     * @throws java.security.cert.CertificateException
     *         Si se produce alguna excepci&oacute;n con los certificados de
     *         firma.
     * @throws AOException
     *         Cuando ocurre cualquier error no contemplado por el resto de
     *         las excepciones declaradas
     */
    byte[] counterSigner(final P7ContentSignerParameters parameters,
                                final byte[] data,
                                final CounterSignTarget targetType,
                                final int[] targets,
                                final PrivateKey key,
                                final java.security.cert.Certificate[] certChain,
                                final String dataType,
                                final Map<String, byte[]> atri,
                                final Map<String, byte[]> uatri) throws IOException, NoSuchAlgorithmException, CertificateException, AOException {

        // Inicializamos el Oid
        // actualOid= dataType;
        this.atrib2 = atri;
        this.uatrib2 = uatri;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
        final ASN1InputStream is = new ASN1InputStream(data);
        final ASN1Sequence dsq = (ASN1Sequence) is.readObject();
        is.close();
        final Enumeration<?> e = dsq.getObjects();
        // Elementos que contienen los elementos OID SignedData
        e.nextElement();
        // Contenido de SignedData
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();
        final ASN1Sequence contentSignedData = (ASN1Sequence) doj.getObject();

        final SignedData sd = SignedData.getInstance(contentSignedData);

        // Obtenemos los signerInfos del SignedData
        final ASN1Set signerInfosSd = sd.getSignerInfos();

        // 4. CERTIFICADOS
        // obtenemos la lista de certificados
        ASN1Set certificates = null;

        final ASN1Set certificatesSigned = sd.getCertificates();
        final ASN1EncodableVector vCertsSig = new ASN1EncodableVector();
        final Enumeration<?> certs = certificatesSigned.getObjects();

        // COGEMOS LOS CERTIFICADOS EXISTENTES EN EL FICHERO
        while (certs.hasMoreElements()) {
            vCertsSig.add((ASN1Encodable) certs.nextElement());
        }

        if (certChain.length != 0) {
            vCertsSig.add(Certificate.getInstance(ASN1Primitive.fromByteArray(certChain[0].getEncoded())));
            certificates = new BERSet(vCertsSig);
        }

        // CRLS no usado
        final ASN1Set certrevlist = null;

        // 5. SIGNERINFO
        // raiz de la secuencia de SignerInfo
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();

        // FIRMA EN ARBOL
        if (targetType.equals(CounterSignTarget.TREE)) {
            signerInfos = counterTree(signerInfosSd, parameters, key, certChain);
        } // FIRMA DE LAS HOJAS
        else if (targetType.equals(CounterSignTarget.LEAFS)) {
            signerInfos = counterLeaf(signerInfosSd, parameters, key, certChain);
        } // FIRMA DE NODOS
        else if (targetType.equals(CounterSignTarget.NODES)) {
            // Firma de Nodos
            SignedData sigDat;
            SignedData aux = sd;

            // int carry = 0;
            int nodo = 0;
            for (int i = targets.length - 1; i >= 0; i--) {
                nodo = targets[i];
                signerInfos = counterNode(aux, parameters, key, certChain, nodo);
                sigDat = new SignedData(sd.getDigestAlgorithms(), sd.getEncapContentInfo(), certificates, certrevlist, new DERSet(signerInfos));

                // Esto se realiza as&iacute; por problemas con los casting.
                final ASN1InputStream sd2 = new ASN1InputStream(sigDat.getEncoded(ASN1Encoding.DER));
                final ASN1Sequence contentSignedData2 = (ASN1Sequence) sd2.readObject();// contenido del SignedData
                sd2.close();

                aux = SignedData.getInstance(contentSignedData2);
            }

            // construimos el Signed Data y lo devolvemos
            return new ContentInfo(PKCSObjectIdentifiers.signedData, aux).getEncoded(ASN1Encoding.DER);
        }
        else if (targetType.equals(CounterSignTarget.SIGNERS)) {
            // Firma de Nodos
            SignedData sigDat;
            SignedData aux = sd;

            int nodo = 0;
            for (int i = targets.length - 1; i >= 0; i--) {
                nodo = targets[i];
                signerInfos = counterNode(
            		aux,
            		parameters,
            		key,
            		certChain,
            		nodo
        		);
                sigDat = new SignedData(sd.getDigestAlgorithms(), sd.getEncapContentInfo(), certificates, certrevlist, new DERSet(signerInfos));

                // Esto se realiza as&iacute; por problemas con los casting.
                final ASN1InputStream sd2 = new ASN1InputStream(sigDat.getEncoded(ASN1Encoding.DER));
                final ASN1Sequence contentSignedData2 = (ASN1Sequence) sd2.readObject();// contenido del SignedData
                sd2.close();

                aux = SignedData.getInstance(contentSignedData2);
            }

            // construimos el Signed Data y lo devolvemos
            return new ContentInfo(PKCSObjectIdentifiers.signedData, aux).getEncoded(ASN1Encoding.DER);
        }

        // construimos el Signed Data y lo devolvemos
        return new ContentInfo(PKCSObjectIdentifiers.signedData, new SignedData(sd.getDigestAlgorithms(),
                                                                                sd.getEncapContentInfo(),
                                                                                certificates,
                                                                                certrevlist,
                                                                                new DERSet(signerInfos))).getEncoded(ASN1Encoding.DER);

    }

    /** M&eacute;todo que contrafirma el arbol completo de forma recursiva, todos
     * los dodos creando un nuevo contraSigner.<br>
     * @param signerInfosRaiz
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo
     * @param key Clave privada a usar para firmar
     * @return El SignerInfo ra&iacute;z con todos sus nodos Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     *         Si no se soporta alguno de los algoritmos de firma o huella
     *         digital
     * @throws java.io.IOException
     *         Si ocurre alg&uacute;n problema leyendo o escribiendo los
     *         datos
     * @throws java.security.cert.CertificateException
     *         Si se produce alguna excepci&oacute;n con los certificados de
     *         firma.
     * @throws es.gob.afirma.exceptions.AOException
     *         Cuando ocurre un error durante el proceso de contrafirma
     *         (formato o clave incorrecto,...) */
    private ASN1EncodableVector counterTree(final ASN1Set signerInfosRaiz,
                                            final P7ContentSignerParameters parameters,
                                            final PrivateKey key,
                                            final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException, IOException, CertificateException, AOException {

        final ASN1EncodableVector counterSigners = new ASN1EncodableVector();

        for (int i = 0; i < signerInfosRaiz.size(); i++) {
            final ASN1Sequence atribute = (ASN1Sequence) signerInfosRaiz.getObjectAt(i);
            final SignerInfo si = new SignerInfo(atribute);
            counterSigners.add(getCounterUnsignedAtributes(si, parameters, key, certChain));
        }

        return counterSigners;
    }

    /** M&eacute;todo que contrafirma las hojas del arbol completo de forma
     * recursiva, todos los dodos creando un nuevo contraSigner.<br>
     * @param signerInfosRaiz
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar
     * @return El SignerInfo ra&iacute;z con todos sus nodos Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private ASN1EncodableVector counterLeaf(final ASN1Set signerInfosRaiz,
                                            final P7ContentSignerParameters parameters,
                                            final PrivateKey key,
                                            final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException, IOException, CertificateException, AOException {

        final ASN1EncodableVector counterSigners = new ASN1EncodableVector();

        for (int i = 0; i < signerInfosRaiz.size(); i++) {
            final ASN1Sequence atribute = (ASN1Sequence) signerInfosRaiz.getObjectAt(i);
            final SignerInfo si = new SignerInfo(atribute);
            counterSigners.add(getCounterLeafUnsignedAtributes(si, parameters, key, certChain));
        }

        return counterSigners;
    }

    /** M&eacute;todo que contrafirma un nodo determinado del arbol buscandolo de
     * forma recursiva.<br>
     * @param sd
     *        SignedData que contiene el Nodo ra&iacute;z.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar
     * @param nodo
     *        Nodo signerInfo a firmar.
     * @return El SignerInfo ra&iacute;z con todos sus nodos Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private ASN1EncodableVector counterNode(final SignedData sd,
                                            final P7ContentSignerParameters parameters,
                                            final PrivateKey key,
                                            final java.security.cert.Certificate[] certChain,
                                            final int nodo) throws NoSuchAlgorithmException, IOException, CertificateException, AOException {

        final ASN1Set signerInfosRaiz = sd.getSignerInfos();

        final ASN1EncodableVector counterSigners = new ASN1EncodableVector();
        ASN1Set auxSignerRaiz;

        auxSignerRaiz = signerInfosRaiz;
        this.actualIndex = 0;

        for (int i = 0; i < auxSignerRaiz.size(); i++) {
            final ASN1Sequence atribute = (ASN1Sequence) auxSignerRaiz.getObjectAt(i);
            final SignerInfo si = new SignerInfo(atribute);
            SignerInfo counterSigner = null;
            if (this.actualIndex == nodo) {
                counterSigner = getCounterNodeUnsignedAtributes(si, parameters, key, certChain);
            }
            else {
                if (this.actualIndex != nodo) {
                    counterSigner = getCounterNodeUnsignedAtributes(si, parameters, key, certChain, nodo);
                }
            }
            this.actualIndex++;
            counterSigners.add(counterSigner);
        }

        return counterSigners;

    }

    /** M&eacute;todo utilizado por la firma del &eacute;rbol para obtener la
     * contrafirma de los signerInfo de forma recursiva.<br>
     * @param signerInfo
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar.
     * @return El SignerInfo ra&iacute;z parcial con todos sus nodos
     *         Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private SignerInfo getCounterUnsignedAtributes(final SignerInfo signerInfo,
                                                   final P7ContentSignerParameters parameters,
                                                   final PrivateKey key,
                                                   final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException,
                                                                            IOException,
                                                                            CertificateException,
                                                                            AOException {

        final List<Object> attributes = new ArrayList<Object>();
        final ASN1EncodableVector signerInfosU = new ASN1EncodableVector();
        final ASN1EncodableVector signerInfosU2 = new ASN1EncodableVector();
        SignerInfo counterSigner = null;
        if (signerInfo.getUnauthenticatedAttributes() != null) {
            final Enumeration<?> eAtributes = signerInfo.getUnauthenticatedAttributes().getObjects();

            while (eAtributes.hasMoreElements()) {
                final Attribute data = Attribute.getInstance(eAtributes.nextElement());
                if (!data.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                    final ASN1Set setInto = data.getAttrValues();
                    final Enumeration<?> eAtributesData = setInto.getObjects();
                    while (eAtributesData.hasMoreElements()) {
                        final Object obj = eAtributesData.nextElement();
                        if (obj instanceof ASN1Sequence) {
                            final ASN1Sequence atrib = (ASN1Sequence) obj;
                            final SignerInfo si = new SignerInfo(atrib);

                            final SignerInfo obtained = getCounterUnsignedAtributes(si, parameters, key, certChain);
                            signerInfosU.add(obtained);
                        }
                        else {
                            attributes.add(obj);
                        }
                    }
                }
                else {
                    signerInfosU.add(data);
                }
            }
            // FIRMA DEL NODO ACTUAL
            counterSigner = unsignedAtributte(parameters, signerInfo, key, certChain);
            signerInfosU.add(counterSigner);

            // FIRMA DE CADA UNO DE LOS HIJOS
            ASN1Set a1;
            final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();
            if (signerInfosU.size() > 1) {
                for (int i = 0; i < signerInfosU.size(); i++) {
                    if (signerInfosU.get(i) instanceof Attribute) {
                        contexExpecific.add(signerInfosU.get(i));
                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(i))));
                    }
                }
                a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                counterSigner =
                        new SignerInfo(signerInfo.getSID(),
                                       signerInfo.getDigestAlgorithm(),
                                       signerInfo.getAuthenticatedAttributes(),
                                       signerInfo.getDigestEncryptionAlgorithm(),
                                       signerInfo.getEncryptedDigest(),
                                       a1 // unsignedAttr
                        );

                // introducido este else pero es sospechoso que no estuviera
                // antes de este ultimo cambio.
            }
            else {
                if (signerInfosU.size() == 1) {
                    if (signerInfosU.get(0) instanceof Attribute) {
                        // anadimos el que hay
                        contexExpecific.add(signerInfosU.get(0));
                        // creamos el de la contrafirma.
                        signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
                        final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
                        contexExpecific.add(uAtrib);

                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(0))));
                    }
                    a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           a1 // unsignedAttr
                            );
                }
                else {
                    // Esta sentencia se comenta para que no se firme el nodo
                    // actual cuando no sea hoja
                    // signerInfosU.add(UnsignedAtributte(parameters, cert,
                    // signerInfo, keyEntry));
                    final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           generateUnsignerInfoFromCounter(uAtrib) // unsignedAttr
                            );
                }
            }
        }
        else {
            signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
            final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
            counterSigner =
                    new SignerInfo(signerInfo.getSID(),
                                   signerInfo.getDigestAlgorithm(),
                                   signerInfo.getAuthenticatedAttributes(),
                                   signerInfo.getDigestEncryptionAlgorithm(),
                                   signerInfo.getEncryptedDigest(),
                                   generateUnsignerInfoFromCounter(uAtrib) // unsignedAttr
                    );

        }
        return counterSigner;
    }

    /** M&eacute;todo utilizado por la firma de una hoja del &eacute;rbol para
     * obtener la contrafirma de los signerInfo de una determinada hoja de forma
     * recursiva.</br>
     * @param signerInfo
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar
     * @return El SignerInfo ra&iacute;z parcial con todos sus nodos
     *         Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private SignerInfo getCounterLeafUnsignedAtributes(final SignerInfo signerInfo,
                                                       final P7ContentSignerParameters parameters,
                                                       final PrivateKey key,
                                                       final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException,
                                                                                                                IOException,
                                                                                                                CertificateException,
                                                                                                                AOException {

        final List<Object> attributes = new ArrayList<Object>();
        final ASN1EncodableVector signerInfosU = new ASN1EncodableVector();
        final ASN1EncodableVector signerInfosU2 = new ASN1EncodableVector();
        SignerInfo counterSigner = null;
        if (signerInfo.getUnauthenticatedAttributes() != null) {
            final Enumeration<?> eAtributes = signerInfo.getUnauthenticatedAttributes().getObjects();

            while (eAtributes.hasMoreElements()) {
                final Attribute data = Attribute.getInstance(eAtributes.nextElement());
                if (!data.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                    final ASN1Set setInto = data.getAttrValues();
                    final Enumeration<?> eAtributesData = setInto.getObjects();

                    while (eAtributesData.hasMoreElements()) {
                        final Object obj = eAtributesData.nextElement();
                        if (obj instanceof ASN1Sequence) {
                            final SignerInfo si = new SignerInfo((ASN1Sequence) obj);
                            final SignerInfo obtained = getCounterLeafUnsignedAtributes(si, parameters, key, certChain);
                            signerInfosU.add(obtained);
                        }
                        else {
                            attributes.add(obj);
                        }
                    }
                }
                else {
                    signerInfosU.add(data);
                }

            }
            // FIRMA DE CADA UNO DE LOS HIJOS
            ASN1Set a1;
            final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();
            if (signerInfosU.size() > 1) {
                for (int i = 0; i < signerInfosU.size(); i++) {
                    if (signerInfosU.get(i) instanceof Attribute) {
                        contexExpecific.add(signerInfosU.get(i));
                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(i))));
                    }
                }
                a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                counterSigner =
                        new SignerInfo(signerInfo.getSID(),
                                       signerInfo.getDigestAlgorithm(),
                                       signerInfo.getAuthenticatedAttributes(),
                                       signerInfo.getDigestEncryptionAlgorithm(),
                                       signerInfo.getEncryptedDigest(),
                                       a1 // unsignedAttr
                        );

            }
            else {
                if (signerInfosU.size() == 1) {
                    if (signerInfosU.get(0) instanceof Attribute) {
                        // anadimos el que hay
                        contexExpecific.add(signerInfosU.get(0));
                        // creamos el de la contrafirma.
                        signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
                        final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
                        contexExpecific.add(uAtrib);

                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(0))));
                    }
                    a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           a1 // unsignedAttr
                            );
                }
                else {
                    // Esta sentencia se comenta para que no se firme el nodo
                    // actual cuando no sea hoja
                    // signerInfosU.add(UnsignedAtributte(parameters, cert,
                    // signerInfo, keyEntry));
                    final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           generateUnsignerInfoFromCounter(uAtrib) // unsignedAttr
                            );
                }

            }
        }
        else {
            signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
            final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
            counterSigner =
                    new SignerInfo(signerInfo.getSID(),
                                   signerInfo.getDigestAlgorithm(),
                                   signerInfo.getAuthenticatedAttributes(),
                                   signerInfo.getDigestEncryptionAlgorithm(),
                                   signerInfo.getEncryptedDigest(),
                                   new DERSet(uAtrib) // unsignedAttr
                    );
        }
        return counterSigner;
    }

    /** M&eacute;todo utilizado por la firma de un nodo del &eacute;rbol para
     * obtener la contrafirma de los signerInfo Sin ser recursivo. Esto es por
     * el caso especial de que puede ser el nodo raiz el nodo a firmar, por lo
     * que no ser&iacute;a necesario usar la recursividad.</br>
     * @param signerInfo
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar
     * @return El SignerInfo ra&iacute;z parcial con todos sus nodos
     *         Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException */
    private SignerInfo getCounterNodeUnsignedAtributes(final SignerInfo signerInfo,
                                                       final P7ContentSignerParameters parameters,
                                                       final PrivateKey key,
                                                       final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException,
                                                                                                                IOException,
                                                                                                                CertificateException {

        final List<Object> attributes = new ArrayList<Object>();
        final ASN1EncodableVector signerInfosU = new ASN1EncodableVector();
        final ASN1EncodableVector signerInfosU2 = new ASN1EncodableVector();
        SignerInfo counterSigner = null;
        if (signerInfo.getUnauthenticatedAttributes() != null) {
            final Enumeration<?> eAtributes = signerInfo.getUnauthenticatedAttributes().getObjects();
            while (eAtributes.hasMoreElements()) {
                final Attribute data = Attribute.getInstance(eAtributes.nextElement());
                if (!data.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                    final ASN1Set setInto = data.getAttrValues();
                    final Enumeration<?> eAtributesData = setInto.getObjects();
                    while (eAtributesData.hasMoreElements()) {
                        final Object obj = eAtributesData.nextElement();
                        if (obj instanceof ASN1Sequence) {
                            signerInfosU.add(new SignerInfo((ASN1Sequence) obj));
                        }
                        else {
                            attributes.add(obj);
                        }
                    }
                }
                else {
                    signerInfosU.add(data);
                }
            }
            // FIRMA DEL NODO ACTUAL
            signerInfosU.add(unsignedAtributte(parameters, signerInfo, key, certChain));

            // FIRMA DE CADA UNO DE LOS HIJOS
            ASN1Set a1;
            final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();
            if (signerInfosU.size() > 1) {
                for (int i = 0; i < signerInfosU.size(); i++) {
                    if (signerInfosU.get(i) instanceof Attribute) {
                        contexExpecific.add(signerInfosU.get(i));
                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(i))));
                    }
                }
                a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                counterSigner =
                        new SignerInfo(signerInfo.getSID(),
                                       signerInfo.getDigestAlgorithm(),
                                       signerInfo.getAuthenticatedAttributes(),
                                       signerInfo.getDigestEncryptionAlgorithm(),
                                       signerInfo.getEncryptedDigest(),
                                       a1 // unsignedAttr
                        );

            }
            else {
                if (signerInfosU.size() == 1) {
                    if (signerInfosU.get(0) instanceof Attribute) {
                        // anadimos el que hay
                        contexExpecific.add(signerInfosU.get(0));
                        // creamos el de la contrafirma.
                        signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
                        final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
                        contexExpecific.add(uAtrib);

                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(0))));
                    }
                    a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           a1 // unsignedAttr
                            );
                }
                else {
                    // Esta sentencia se comenta para que no se firme el nodo
                    // actual cuando no sea hoja
                    // signerInfosU.add(UnsignedAtributte(parameters, cert,
                    // signerInfo, keyEntry));
                    final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           generateUnsignerInfoFromCounter(uAtrib) // unsignedAttr
                            );
                }

            }
        }
        else {
            signerInfosU2.add(unsignedAtributte(parameters, signerInfo, key, certChain));
            final Attribute uAtrib = new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU2));
            counterSigner =
                    new SignerInfo(signerInfo.getSID(),
                                   signerInfo.getDigestAlgorithm(),
                                   signerInfo.getAuthenticatedAttributes(),
                                   signerInfo.getDigestEncryptionAlgorithm(),
                                   signerInfo.getEncryptedDigest(),
                                   new DERSet(uAtrib) // unsignedAttr
                    );
        }
        return counterSigner;
    }

    /** M&eacute;todo utilizado por la firma de un nodo del &eacute;rbol para
     * obtener la contrafirma de los signerInfo buscando el nodo de forma
     * recursiva.</br>
     * @param signerInfo
     *        Nodo ra&iacute; que contiene todos los signerInfos que se
     *        deben firmar.
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param key Clave privada a usar para firmar
     * @param node
     *        Nodo espec&iacute;fico a firmar.
     * @return El SignerInfo ra&iacute;z parcial con todos sus nodos
     *         Contrafirmados.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private SignerInfo getCounterNodeUnsignedAtributes(final SignerInfo signerInfo,
                                                       final P7ContentSignerParameters parameters,
                                                       final PrivateKey key,
                                                       final java.security.cert.Certificate[] certChain,
                                                       final int node) throws NoSuchAlgorithmException, IOException, CertificateException, AOException {

        final List<Object> attributes = new ArrayList<Object>();
        final ASN1EncodableVector signerInfosU = new ASN1EncodableVector();
        SignerInfo counterSigner = null;
        if (signerInfo.getUnauthenticatedAttributes() != null) {
            final Enumeration<?> eAtributes = signerInfo.getUnauthenticatedAttributes().getObjects();
            while (eAtributes.hasMoreElements()) {
                final Attribute data = Attribute.getInstance(eAtributes.nextElement());
                if (!data.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                    final ASN1Set setInto = data.getAttrValues();
                    final Enumeration<?> eAtributesData = setInto.getObjects();
                    while (eAtributesData.hasMoreElements()) {
                        final Object obj = eAtributesData.nextElement();
                        if (obj instanceof ASN1Sequence) {
                            final ASN1Sequence atrib = (ASN1Sequence) obj;
                            final SignerInfo si = new SignerInfo(atrib);
                            this.actualIndex++;
                            if (this.actualIndex != node) {
                                if (this.actualIndex < node) {
                                    signerInfosU.add(getCounterNodeUnsignedAtributes(si, parameters, key, certChain, node));
                                }
                                else {
                                    signerInfosU.add(si);
                                }
                            }
                            else {
                                signerInfosU.add(getCounterNodeUnsignedAtributes(si, parameters, key, certChain));
                            }
                        }
                        else {
                            attributes.add(obj);
                        }
                    }
                }
                else {
                    signerInfosU.add(data);
                }

            }
            // FIRMA DE CADA UNO DE LOS HIJOS
            ASN1Set a1;
            final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();
            if (signerInfosU.size() > 1) {
                for (int i = 0; i < signerInfosU.size(); i++) {
                    if (signerInfosU.get(i) instanceof Attribute) {
                        contexExpecific.add(signerInfosU.get(i));
                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(i))));
                    }
                }
                a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                counterSigner =
                        new SignerInfo(signerInfo.getSID(),
                                       signerInfo.getDigestAlgorithm(),
                                       signerInfo.getAuthenticatedAttributes(),
                                       signerInfo.getDigestEncryptionAlgorithm(),
                                       signerInfo.getEncryptedDigest(),
                                       a1 // unsignedAttr
                        );

            }
            else {
                if (signerInfosU.size() == 1) {
                    if (signerInfosU.get(0) instanceof Attribute) {
                        // anadimos el que hay
                        contexExpecific.add(signerInfosU.get(0));
                        // creamos el de la contrafirma.

                    }
                    else {
                        contexExpecific.add(new Attribute(CMSAttributes.counterSignature, new DERSet(signerInfosU.get(0))));
                    }
                    a1 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));
                    counterSigner =
                            new SignerInfo(signerInfo.getSID(),
                                           signerInfo.getDigestAlgorithm(),
                                           signerInfo.getAuthenticatedAttributes(),
                                           signerInfo.getDigestEncryptionAlgorithm(),
                                           signerInfo.getEncryptedDigest(),
                                           a1 // unsignedAttr
                            );
                }

            }
        }
        else {

            counterSigner =
                    new SignerInfo(signerInfo.getSID(),
                                   signerInfo.getDigestAlgorithm(),
                                   signerInfo.getAuthenticatedAttributes(),
                                   signerInfo.getDigestEncryptionAlgorithm(),
                                   signerInfo.getEncryptedDigest(),
                                   null // unsignedAttr
                    );

        }
        return counterSigner;
    }

    /** M&eacute;todo que genera la parte que contiene la informaci&oacute;n del
     * Usuario. Se generan los atributos que se necesitan para generar la
     * firma.</br>
     * @param cert
     *        Certificado necesario para la firma.
     * @param digestAlgorithm
     *        Algoritmo Firmado.
     * @param datos
     *        Datos firmados.
     * @return Los datos necesarios para generar la firma referente a los datos
     *         del usuario.
     * @throws java.security.NoSuchAlgorithmException */
    private ASN1Set generateSignerInfo(final X509Certificate cert, final String digestAlgorithm, final byte[] datos) throws NoSuchAlgorithmException {

        // // ATRIBUTOS

        // authenticatedAttributes
        final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();

        // Las Contrafirmas CMS no tienen ContentType

        // fecha de firma
        contexExpecific.add(new Attribute(CMSAttributes.signingTime, new DERSet(new DERUTCTime(new Date()))));

        // MessageDigest
        contexExpecific.add(new Attribute(CMSAttributes.messageDigest,
                                          new DERSet(new DEROctetString(MessageDigest.getInstance(AOSignConstants.getDigestAlgorithmName(digestAlgorithm)).digest(datos)))));

        // Serial Number
        contexExpecific.add(new Attribute(RFC4519Style.serialNumber, new DERSet(new DERPrintableString(cert.getSerialNumber().toString()))));

        // agregamos la lista de atributos a mayores.
        if (this.atrib2.size() != 0) {
            final Iterator<Map.Entry<String, byte[]>> it = this.atrib2.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, byte[]> e = it.next();
                contexExpecific.add(new Attribute(
                  new ASN1ObjectIdentifier(e.getKey().toString()), // el oid
                  new DERSet(new DERPrintableString(new String(e.getValue()))))); // el array de bytes en formato string
            }
        }

        this.signedAttr2 = SigUtils.getAttributeSet(new AttributeTable(contexExpecific));

        return SigUtils.getAttributeSet(new AttributeTable(contexExpecific));

    }

    /** M&eacute;todo que genera la parte que contiene la informaci&oacute;n del
     * Usuario. Se generan los atributos no firmados.
     * @return Los atributos no firmados de la firma. */
    private ASN1Set generateUnsignerInfo() {

        // // ATRIBUTOS

        // authenticatedAttributes
        final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();

        // agregamos la lista de atributos a mayores.
        if (this.uatrib2.size() != 0) {
            final Iterator<Map.Entry<String, byte[]>> it = this.uatrib2.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, byte[]> e = it.next();
                contexExpecific.add(
                    new Attribute(
                          // el oid
                          new ASN1ObjectIdentifier(e.getKey().toString()),
                          // el array de bytes en formato string
                          new DERSet(new DERPrintableString(new String(e.getValue())))
                    )
                );
            }
        }
        else {
            return null;
        }

        return SigUtils.getAttributeSet(new AttributeTable(contexExpecific));

    }

    /** M&eacute;todo que genera la parte que contiene la informaci&oacute;n del
     * Usuario. Se generan los atributos no firmados.
     * @param uAtrib
     *        Lista de atributos no firmados que se insertar&aacute;n dentro
     *        del archivo de firma.
     * @return Los atributos no firmados de la firma. */
    private ASN1Set generateUnsignerInfoFromCounter(final Attribute uAtrib) {

        // // ATRIBUTOS

        // authenticatedAttributes
        final ASN1EncodableVector contexExpecific = new ASN1EncodableVector();

        // agregamos la lista de atributos a mayores.
        if (this.uatrib2.size() != 0) {
            final Iterator<Map.Entry<String, byte[]>> it = this.uatrib2.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, byte[]> e = it.next();
                contexExpecific.add(new Attribute(
            		// el oid
                    new ASN1ObjectIdentifier(e.getKey().toString()),
                    // el array de bytes en formato string
                    new DERSet(new DERPrintableString(new String(e.getValue()))))
                );
            }
        }
        contexExpecific.add(uAtrib);

        return SigUtils.getAttributeSet(new AttributeTable(contexExpecific));

    }

    /** M&eacute;todo que genera un signerInfo espec&iacute;fico utilizando los
     * datos necesarios para crearlo. Se utiliza siempre que no se sabe cual es
     * el signerInfo que se debe firmar.</br>
     * @param parameters
     *        Par&aacute;metros necesarios para firmar un determinado
     *        SignerInfo hoja.
     * @param si
     *        SignerInfo del que se debe recoger la informaci&oacute;n para
     *        realizar la contrafirma espec&iacute;fica.
     * @param key Clave privada a usar para firmar
     * @return El signerInfo contrafirmado.
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.IOException
     * @throws java.security.cert.CertificateException */
    private SignerInfo unsignedAtributte(final P7ContentSignerParameters parameters,
                                         final SignerInfo si,
                                         final PrivateKey key,
                                         final java.security.cert.Certificate[] certChain) throws NoSuchAlgorithmException,
                                                                                                  IOException,
                                                                                                  CertificateException {
        // // UNAUTHENTICATEDATTRIBUTES

        // buscamos que timo de algoritmo es y lo codificamos con su OID
        final String signatureAlgorithm = parameters.getSignatureAlgorithm();
        final String digestAlgorithm = AOSignConstants.getDigestAlgorithmName(signatureAlgorithm);

        // ATRIBUTOS FINALES

        final ASN1Set signedAttr = generateSignerInfo((X509Certificate) certChain[0], digestAlgorithm, si.getEncryptedDigest().getOctets());
        final ASN1Set unsignedAttr = generateUnsignerInfo();

        // 5. SIGNERINFO
        // raiz de la secuencia de SignerInfo
        final TBSCertificateStructure tbs = TBSCertificateStructure.getInstance(ASN1Primitive.fromByteArray(((X509Certificate)certChain[0]).getTBSCertificate()));
        final IssuerAndSerialNumber encSid = new IssuerAndSerialNumber(X500Name.getInstance(tbs.getIssuer()), tbs.getSerialNumber().getValue());
        final SignerIdentifier identifier = new SignerIdentifier(encSid);

        // AlgorithmIdentifier
        final AlgorithmIdentifier digAlgId = SigUtils.makeAlgId(AOAlgorithmID.getOID(digestAlgorithm));

        // // FIN ATRIBUTOS

        // digEncryptionAlgorithm
        final AlgorithmIdentifier encAlgId = SigUtils.makeAlgId(AOAlgorithmID.getOID("RSA")); //$NON-NLS-1$

        final ASN1OctetString sign2;
        try {
            sign2 = firma(signatureAlgorithm, key);
        }
        catch (final Exception ex) {
            throw new IOException("Error realizando la firma: " + ex, ex); //$NON-NLS-1$
        }

        return new SignerInfo(identifier, digAlgId, signedAttr, encAlgId, sign2, unsignedAttr);

    }

    /** Realiza la firma usando los atributos del firmante.
     * @param signatureAlgorithm
     *        Algoritmo para la firma
     * @param key Clave para firmar.
     * @return Firma de los atributos.
     * @throws es.map.es.map.afirma.exceptions.AOException */
    private ASN1OctetString firma(final String signatureAlgorithm, final PrivateKey key) throws AOException {

        final Signature sig;
        try {
            sig = Signature.getInstance(signatureAlgorithm);
        }
        catch (final Exception e) {
            throw new AOException("Error obteniendo la clase de firma para el algoritmo " + signatureAlgorithm, e); //$NON-NLS-1$
        }

        final byte[] tmp;
        try {
            tmp = this.signedAttr2.getEncoded(ASN1Encoding.DER);
        }
        catch (final IOException ex) {
            throw new AOException("Error obteniendo los datos a firmar", ex); //$NON-NLS-1$
        }

        // Indicar clave privada para la firma
        try {
            sig.initSign(key);
        }
        catch (final Exception e) {
            throw new AOException("Error al inicializar la firma con la clave privada", e); //$NON-NLS-1$
        }

        // Actualizamos la configuracion de firma
        try {
            sig.update(tmp);
        }
        catch (final SignatureException e) {
            throw new AOException("Error al configurar la informacion de firma", e); //$NON-NLS-1$
        }

        // firmamos.
        final byte[] realSig;
        try {
            realSig = sig.sign();
        }
        catch (final Exception e) {
            throw new AOException("Error durante el proceso de firma", e); //$NON-NLS-1$
        }

        return new DEROctetString(realSig);

    }

}