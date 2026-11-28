export interface TokenStorageAdapter {
  getToken(): string | null;
  setToken(token: string | null): void;
  getUser(): string | null;
  setUser(userJson: string | null): void;
  clear(): void;
}

class SessionStorageAdapter implements TokenStorageAdapter {
  private tokenKey = 'cg_admin_token';
  private userKey = 'cg_admin_user';

  getToken(): string | null {
    try {
      return sessionStorage.getItem(this.tokenKey);
    } catch {
      return null;
    }
  }

  setToken(token: string | null): void {
    try {
      if (token) {
        sessionStorage.setItem(this.tokenKey, token);
      } else {
        sessionStorage.removeItem(this.tokenKey);
      }
    } catch (e) {
      console.warn('SessionStorage not available', e);
    }
  }

  getUser(): string | null {
    try {
      return sessionStorage.getItem(this.userKey);
    } catch {
      return null;
    }
  }

  setUser(userJson: string | null): void {
    try {
      if (userJson) {
        sessionStorage.setItem(this.userKey, userJson);
      } else {
        sessionStorage.removeItem(this.userKey);
      }
    } catch (e) {
      console.warn('SessionStorage not available', e);
    }
  }

  clear(): void {
    try {
      sessionStorage.removeItem(this.tokenKey);
      sessionStorage.removeItem(this.userKey);
    } catch (e) {
      console.warn('SessionStorage clear failed', e);
    }
  }
}

class InMemoryStorageAdapter implements TokenStorageAdapter {
  private token: string | null = null;
  private user: string | null = null;

  getToken(): string | null {
    return this.token;
  }

  setToken(token: string | null): void {
    this.token = token;
  }

  getUser(): string | null {
    return this.user;
  }

  setUser(userJson: string | null): void {
    this.user = userJson;
  }

  clear(): void {
    this.token = null;
    this.user = null;
  }
}

// Default storage adapter: SessionStorage (pluggable/swappable when TV5 confirms Auth Contract)
let currentAdapter: TokenStorageAdapter = new SessionStorageAdapter();

export const tokenStorage = {
  getAdapter(): TokenStorageAdapter {
    return currentAdapter;
  },
  setAdapter(adapter: TokenStorageAdapter): void {
    currentAdapter = adapter;
  },
  useInMemory(): void {
    currentAdapter = new InMemoryStorageAdapter();
  },
  useSessionStorage(): void {
    currentAdapter = new SessionStorageAdapter();
  },
  getToken(): string | null {
    return currentAdapter.getToken();
  },
  setToken(token: string | null): void {
    currentAdapter.setToken(token);
  },
  getUser(): string | null {
    return currentAdapter.getUser();
  },
  setUser(userJson: string | null): void {
    currentAdapter.setUser(userJson);
  },
  clear(): void {
    currentAdapter.clear();
  }
};
