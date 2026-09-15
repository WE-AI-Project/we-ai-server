package com.weai.server.domain.smartcommit.domain;

public enum SynCommitType {

	/** Generated automatically by the scheduler once staged changes go idle. */
	AUTO,

	/** Triggered on demand via the "syn commit" API. */
	MANUAL
}
