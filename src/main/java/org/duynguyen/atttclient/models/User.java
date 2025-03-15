package org.duynguyen.atttclient.models;

import lombok.Builder;

@Builder
public record User(int id, String username) {
}
