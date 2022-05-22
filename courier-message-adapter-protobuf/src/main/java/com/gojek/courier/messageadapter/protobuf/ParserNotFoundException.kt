package com.gojek.courier.messageadapter.protobuf

class ParserNotFoundException(clazz: Class<*>) : IllegalArgumentException(
    "Found a protobuf message but ${clazz.name} had no parser() method or PARSER field."
)
