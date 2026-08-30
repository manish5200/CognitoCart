import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/products';
  private readonly CAT_API = 'https://cognitocart-api.onrender.com/api/v1/categories';

  constructor(private http: HttpClient) {}

  // --- Public ---
  getAll(page = 0, size = 20): Observable<any> {
    return this.http.get(`${this.API}?page=${page}&size=${size}`);
  }

  getBySlug(slug: string): Observable<any> {
    return this.http.get(`${this.API}/${slug}`);
  }

  getByCategory(categoryPublicId: string, page = 0, size = 20): Observable<any> {
    return this.http.get(`${this.API}/category/${categoryPublicId}?page=${page}&size=${size}`);
  }

  search(params: {
    name?: string; categoryId?: string; minPrice?: number;
    maxPrice?: number; minRating?: number; page?: number; size?: number; sortBy?: string; direction?: string;
  }): Observable<any> {
    let p = new HttpParams();
    if (params.name) p = p.set('name', params.name);
    if (params.categoryId) p = p.set('categoryId', params.categoryId);
    if (params.minPrice != null) p = p.set('minPrice', params.minPrice.toString());
    if (params.maxPrice != null) p = p.set('maxPrice', params.maxPrice.toString());
    if (params.minRating != null) p = p.set('minRating', params.minRating.toString());
    p = p.set('page', (params.page ?? 0).toString());
    p = p.set('size', (params.size ?? 20).toString());
    if (params.sortBy) p = p.set('sortBy', params.sortBy);
    if (params.direction) p = p.set('direction', params.direction);
    return this.http.get(`${this.API}/search`, { params: p });
  }

  // AI semantic search â€” converts a plain-English query into vector search results.
  // Optionally narrows results by price range or minimum rating (hybrid search).
  // Returns: { query, totalFound, results: [{product, relevanceScore, relevanceLabel, rank}] }
  semanticSearch(
    query: string,
    limit = 20,
    filters?: { minPrice?: number; maxPrice?: number; minRating?: number }
  ): Observable<any> {
    // Build query params dynamically â€” only attach filters that have actual values
    let p = new HttpParams()
      .set('q', query)
      .set('limit', limit.toString());

    if (filters?.minPrice != null) p = p.set('minPrice', filters.minPrice.toString());
    if (filters?.maxPrice != null) p = p.set('maxPrice', filters.maxPrice.toString());
    if (filters?.minRating != null) p = p.set('minRating', filters.minRating.toString());

    return this.http.get(`${this.API}/search/semantic`, { params: p });
  }

  // Admin endpoint to manually trigger a re-index of products missing AI embeddings
  reindexAi(): Observable<any> {
    return this.http.post(`${this.API}/admin/reindex`, {});
  }

  getReturnPolicy(productPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/${productPublicId}/return-policy`);
  }

  createReturnPolicy(payload: any): Observable<any> {
    return this.http.post(`${this.API}/return-policy`, payload);
  }

  updateReturnPolicy(policyPublicId: string, payload: any): Observable<any> {
    return this.http.put(`${this.API}/return-policy/${policyPublicId}`, payload);
  }

  getVariants(productPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/${productPublicId}/variants`);
  }

  createVariant(productPublicId: string, payload: any): Observable<any> {
    return this.http.post(`${this.API}/${productPublicId}/variants`, payload);
  }

  updateVariant(productPublicId: string, variantPublicId: string, payload: any): Observable<any> {
    return this.http.put(`${this.API}/${productPublicId}/variants/${variantPublicId}`, payload);
  }

  toggleVariantStatus(productPublicId: string, variantPublicId: string): Observable<any> {
    return this.http.patch(`${this.API}/${productPublicId}/variants/${variantPublicId}/status`, {});
  }

  // --- Categories ---
  getCategories(): Observable<any> {
    return this.http.get(this.CAT_API);
  }

  addCategory(category: { name: string; parentCategory?: { publicId: string } }): Observable<any> {
    return this.http.post(this.CAT_API, category);
  }

  updateCategory(publicId: string, category: { name: string; parentCategory?: { publicId: string } }): Observable<any> {
    return this.http.put(`${this.CAT_API}/${publicId}`, category);
  }

  deleteCategory(publicId: string): Observable<any> {
    return this.http.delete(`${this.CAT_API}/${publicId}`);
  }

  // --- Seller ---
  create(data: any): Observable<any> {
    return this.http.post(this.API, data);
  }

  update(productPublicId: string, data: any): Observable<any> {
    return this.http.put(`${this.API}/${productPublicId}`, data);
  }

  uploadImage(productPublicId: string, file: File): Observable<any> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post(`${this.API}/${productPublicId}/upload-image`, form);
  }

  deleteImage(productPublicId: string, publicId: string): Observable<any> {
    return this.http.delete(`${this.API}/${productPublicId}/images?publicId=${encodeURIComponent(publicId)}`);
  }

  toggle(productPublicId: string): Observable<any> {
    return this.http.patch(`${this.API}/${productPublicId}/toggle`, {});
  }

  delete(productPublicId: string): Observable<any> {
    return this.http.delete(`${this.API}/${productPublicId}`);
  }
}
