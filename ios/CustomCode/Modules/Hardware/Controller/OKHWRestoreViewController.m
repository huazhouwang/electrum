//
//  OKHWRestoreViewController.m
//  OneKey
//
//  Created by xiaoliang on 2021/1/18.
//  Copyright © 2021 Onekey. All rights reserved.
//

#import "OKHWRestoreViewController.h"
#import "OKFindWalletTableViewCell.h"
#import "OKFindWalletTableViewCellModel.h"
#import "OKBiologicalViewController.h"


@interface OKHWRestoreViewController ()<UITableViewDelegate,UITableViewDataSource>

@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UIView *tableViewBgView;
@property (weak, nonatomic) IBOutlet UITableView *tbaleView;
@property (weak, nonatomic) IBOutlet UIView *headerView;
@property (weak, nonatomic) IBOutlet UIButton *restoreBtn;
- (IBAction)restoreBtnClick:(UIButton *)sender;
@property (weak, nonatomic) IBOutlet UILabel *descLabel;
@property (nonatomic,strong)NSArray *walletList;
@property (weak, nonatomic) IBOutlet UIImageView *quanquanView;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomBgViewCons;
@property (nonatomic,strong)OKCreateResultModel *createResultModel;
@property (nonatomic,assign)OKRestoreRefreshUI type;
@end

@implementation OKHWRestoreViewController
+ (instancetype)restoreViewController
{
    return [[UIStoryboard storyboardWithName:@"Hardware" bundle:nil]instantiateViewControllerWithIdentifier:@"OKHWRestoreViewController"];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self hideBackBtn];
    self.tableView.tableFooterView = [UIView new];
    self.title = MyLocalizedString(@"Restore the purse", nil);
    self.titleLabel.text = MyLocalizedString(@"Find the following wallet", nil);
    self.restoreBtn.hidden = YES;
    _type = OKRestoreRefreshUISearch;
    [self refreshUI];
    [self rotateImageView];
    [self.tableViewBgView setLayerRadius:20];
    [self restoreWallet];
}
- (void)restoreWallet
{
    OKWeakSelf(self)
    __block NSDictionary* create = nil;
    [OKHwNotiManager  sharedInstance].delegate = self;
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
       NSString *xpub = [kPyCommandsManager callInterface:kInterfacecreate_hw_derived_wallet parameter:@{}];
        NSArray *array = @[@[xpub,kOKBlueManager.currentConnectModel.device_id]];
        NSString *xpubs = [array mj_JSONString];
        create = [kPyCommandsManager callInterface:kInterfaceimport_create_hw_wallet parameter:@{@"name":@"",@"m":@"1",@"n":@"1",@"xpubs":xpubs,@"hd":@"1"}];
        dispatch_async(dispatch_get_main_queue(), ^{
            [weakself createComplete:create];
        });
    });
}
- (void)createComplete:(NSDictionary *)create
{
    NSLog(@"create == %@",create);
    OKWeakSelf(self)
    if (create != nil) {
        OKCreateResultModel *createResultModel = [OKCreateResultModel mj_objectWithKeyValues:create];
        if (createResultModel.derived_info.count > 0) {
            weakself.walletList =  createResultModel.derived_info;
            [weakself.tableView reloadData];
            _type = OKRestoreRefreshUIHaveWallet;
            [weakself refreshUI];
        }else{
            _type = OKRestoreRefreshUIZeroWallet;
            [weakself refreshUI];
        }
    }
}

- (void)refreshUI
{
    switch (_type) {
        case OKRestoreRefreshUISearch:
        {
            self.quanquanView.hidden = NO;
            self.bottomBgViewCons.constant = 28;
            self.restoreBtn.hidden = YES;
            self.descLabel.text = MyLocalizedString(@"Search your wallet...", nil);
            [self.view layoutIfNeeded];
        }
            break;
        case OKRestoreRefreshUIZeroWallet:
        {
            self.quanquanView.hidden = YES;
            self.bottomBgViewCons.constant = 200;
            self.restoreBtn.hidden = NO;
            self.descLabel.text = MyLocalizedString(@"No wallet", nil);
            [self.restoreBtn setTitle:MyLocalizedString(@"return", nil) forState:UIControlStateNormal];
            [self.view layoutIfNeeded];
        }
            break;
        case OKRestoreRefreshUIHaveWallet:
        {
            self.quanquanView.hidden = YES;
            self.bottomBgViewCons.constant = 200;
            self.restoreBtn.hidden = NO;
            self.descLabel.text = MyLocalizedString(@"You have created these wallets for this App, select which you want to restore", nil);
            [self.restoreBtn setTitle:MyLocalizedString(@"restore", nil) forState:UIControlStateNormal];
            [self.view layoutIfNeeded];
        }
            break;
        default:
            break;
    }
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    if ([self.navigationController respondsToSelector:@selector(interactivePopGestureRecognizer)]){
        self.navigationController.interactivePopGestureRecognizer.enabled = NO;
    }
}
- (void)viewDidDisappear:(BOOL)animated {
  [super viewDidDisappear:animated];
  self.navigationController.interactivePopGestureRecognizer.enabled = YES;
}

- (void)rotateImageView {
    OKWeakSelf(self)
    CGFloat circleByOneSecond = 2.5f;
    [UIView animateWithDuration:1.f / circleByOneSecond
                          delay:0
                        options:UIViewAnimationOptionCurveLinear
                     animations:^{
        weakself.quanquanView.transform = CGAffineTransformRotate(weakself.quanquanView.transform, M_PI_2);
    }
                     completion:^(BOOL finished){
        [weakself rotateImageView];
    }];
}
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return self.walletList.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *ID = @"OKFindWalletTableViewCell";
    OKFindWalletTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:ID];
    if (cell == nil) {
        cell = [[OKFindWalletTableViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:ID];
    }
    cell.model = self.walletList[indexPath.row];
    return  cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 90;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    OKFindWalletTableViewCellModel *model = self.walletList[indexPath.row];
    model.isSelected = !model.isSelected;
    [self.tableView reloadData];
}

- (IBAction)restoreBtnClick:(UIButton *)sender {
    switch (_type) {
        case OKRestoreRefreshUIHaveWallet:
        {
            NSMutableArray *arrayM = [NSMutableArray array];
            for (OKFindWalletTableViewCellModel *model in self.walletList) {
                if (model.isSelected) {
                    [arrayM addObject:model.name];
                }
            }
            id result = [kPyCommandsManager callInterface:kInterfacerecovery_confirmed parameter:@{@"name_list":arrayM,@"hw":@"1"}];
            if (result != nil) {
                NSString *selectName = [arrayM firstObject];
                OKWalletInfoModel *infoModel = [kWalletManager getCurrentWalletAddress:selectName];
                [kWalletManager setCurrentWalletInfo:infoModel];
                [kTools tipMessage:MyLocalizedString(@"Restore success", nil)];
                [self.OK_TopViewController dismissToViewControllerWithClassName:@"OKWalletViewController" animated:YES complete:^{
                    [[NSNotificationCenter defaultCenter]postNotificationName:kNotiWalletCreateComplete object:@{@"backupshow":@"0",@"takecareshow":@"0"}];
                }];
            }
        }
            break;
        case OKRestoreRefreshUIZeroWallet:
        {
            [self.navigationController popViewControllerAnimated:YES];
        }
            break;
        default:
            break;
    }
}
@end
