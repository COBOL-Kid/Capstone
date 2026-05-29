import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { apiConfig } from '../api/api.config';
import { RecallService } from './recall.service';

describe('RecallService', () => {
  let service: RecallService;
  let httpTesting: HttpTestingController;
  const vin = 'JTENU5JR6M5962554';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RecallService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('posts completed recalls and deletes completion records', () => {
    const completed = {
      completedRecallId: 55,
      vin,
      recallId: 22,
      completedDate: '2025-04-06',
      repairShop: 'Dealer',
      cost: 0,
      notes: null,
      nhtsaCampaignNumber: '22V480000',
      reportReceivedDate: '2022-06-07',
      component: 'Airbag',
      summary: 'Summary',
      consequence: 'Consequence',
      remedy: 'Remedy',
    };

    service
      .completeRecall({ vin, recallId: 22 })
      .subscribe((response) => expect(response).toEqual(completed));

    const postRequest = httpTesting.expectOne(`${apiConfig.recallUrl}/completed`);
    postRequest.flush(completed);

    service.uncompleteRecall(55).subscribe();
    const deleteRequest = httpTesting.expectOne(`${apiConfig.recallUrl}/completed/55`);
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null, { status: 204, statusText: 'No Content' });
  });
});
