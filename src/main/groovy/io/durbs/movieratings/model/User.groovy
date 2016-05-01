package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.BsonDocument
import org.bson.BsonDocumentWrapper
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.types.ObjectId

@Canonical
@CompileStatic
class User implements Bson {

  ObjectId id

  String username
  String emailAddress
  String password

  @Override
  <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {

    new BsonDocumentWrapper<User>(this, codecRegistry.get(User))
  }
}
