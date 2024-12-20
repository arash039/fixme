package com.fixme;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

final class ClientAttachment {
	ByteBuffer buffer = ByteBuffer.allocate(4096);
	AsynchronousSocketChannel client;
	String id;

	ClientAttachment(AsynchronousSocketChannel client, String id) {
		this.client = client;
		this.id = id;
	}
}
