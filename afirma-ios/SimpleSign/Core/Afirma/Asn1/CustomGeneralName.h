/*
 * Generated by asn1c-0.9.21 (http://lionet.info/asn1c)
 * From ASN.1 module "SIGNEDDATA"
 * 	found in "SIGNEDDATA.asn1"
 */

#ifndef	_CustomGeneralName_H_
#define	_CustomGeneralName_H_


#include "asn_application.h"

/* Including external dependencies */
#include "asn_SET_OF.h"
#include "constr_SET_OF.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations */
struct Name;

/* CustomGeneralName */
typedef struct CustomGeneralName {
	A_SET_OF(struct Name) list;
	
	/* Context for parsing across buffer boundaries */
	asn_struct_ctx_t _asn_ctx;
} CustomGeneralName_t;

/* Implementation */
extern asn_TYPE_descriptor_t asn_DEF_CustomGeneralName;

#ifdef __cplusplus
}
#endif

/* Referred external types */
#include "Name.h"

#endif	/* _CustomGeneralName_H_ */