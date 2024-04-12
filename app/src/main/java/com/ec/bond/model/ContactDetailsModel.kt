package com.ec.bond.model

class ContactDetailsModel {
    var contact_name: String? = null
    var contact_number: String? = null

    constructor(contact_name: String, contact_number: String) {
        this.contact_name = contact_name
        this.contact_number = contact_number
    }


}