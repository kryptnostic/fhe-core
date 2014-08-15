package com.kryptnostic.indexing.metadata;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class BaseMetadatum implements Metadatum {
	private final String documentId;
	private final String token;
	private final List<Integer> locations;
	
	//TODO: Add builder 
	public BaseMetadatum( String documentId , String token , Iterable<Integer> locations ) {
		this.documentId = documentId;
		this.token = token;
		this.locations = ImmutableList.copyOf( locations );
	}
	
	@Override
	public String getDocumentId() {
		return documentId;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public List<Integer> getLocations() {
		return locations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((documentId == null) ? 0 : documentId.hashCode());
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals(Metadatum obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof BaseMetadatum)) {
			return false;
		}
		BaseMetadatum other = (BaseMetadatum) obj;
		if (documentId == null) {
			if (other.documentId != null) {
				return false;
			}
		} else if (!documentId.equals(other.documentId)) {
			return false;
		}
		if (locations == null) {
			if (other.locations != null) {
				return false;
			}
		} else if (!locations.equals(other.locations)) {
			return false;
		}
		if (token == null) {
			if (other.token != null) {
				return false;
			}
		} else if (!token.equals(other.token)) {
			return false;
		}
		return true;
	}

    @Override
    public String toString() {
        return "BaseMetadatum [documentId=" + documentId + ", token=" + token + ", locations=" + locations + "]";
    }

}
