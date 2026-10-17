package com.shubh3011.facesecurity.model;

/** An enrolled embedding joined with the identity that owns it. */
public record StoredFace(long personId, String personName, FaceEmbedding embedding) {
}
